package abzu.ast.call;

import abzu.AbzuException;
import abzu.AbzuLanguage;
import abzu.ast.ExpressionNode;
import abzu.ast.controlflow.BlockNode;
import abzu.ast.expression.SimpleIdentifierNode;
import abzu.ast.expression.value.FunctionNode;
import abzu.ast.local.ReadArgumentNode;
import abzu.ast.local.WriteLocalVariableNodeGen;
import abzu.runtime.Function;
import abzu.runtime.async.Promise;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * The node for function invocation in Abzu. Since Abzu has first class functions, the {@link abzu.runtime.Function
 * target function} can be computed by an arbitrary expression. This node is responsible for
 * evaluating this expression, as well as evaluating the {@link #argumentNodes arguments}. The
 * actual dispatch is then delegated to a chain of {@link DispatchNode} that form a polymorphic
 * inline cache.
 */
@NodeInfo(shortName = "invoke")
public final class InvokeNode extends ExpressionNode {

  @Node.Child
  private ExpressionNode functionNode;
  private final Function function;
  @Node.Children
  private final ExpressionNode[] argumentNodes;
  @Node.Child
  private DispatchNode dispatchNode;

  private AbzuLanguage language;

  public InvokeNode(AbzuLanguage language, ExpressionNode functionNode, ExpressionNode[] argumentNodes) {
    this.functionNode = functionNode;
    this.function = null;
    this.argumentNodes = argumentNodes;
    this.dispatchNode = DispatchNodeGen.create();
    this.language = language;
  }

  public InvokeNode(AbzuLanguage language, Function function, ExpressionNode[] argumentNodes) {
    this.functionNode = null;
    this.function = function;
    this.argumentNodes = argumentNodes;
    this.dispatchNode = DispatchNodeGen.create();
    this.language = language;
  }

  @Override
  public String toString() {
    return "InvokeNode{" +
        "functionNode=" + functionNode +
        ", function=" + function +
        ", argumentNodes=" + Arrays.toString(argumentNodes) +
        '}';
  }

  @ExplodeLoop
  @Override
  public Object executeGeneric(VirtualFrame frame) {
    final Function function;
    if (this.function != null) {
      function = this.function;
    } else {
      try {
        function = functionNode.executeFunction(frame);
      } catch (UnexpectedResultException e) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new AbzuException("Cannot invoke non-function node: " + functionNode, this);
      }
    }

    /*
     * The number of arguments is constant for one invoke node. During compilation, the loop is
     * unrolled and the execute methods of all arguments are inlined. This is triggered by the
     * ExplodeLoop annotation on the method. The compiler assertion below illustrates that the
     * array length is really constant.
     */
    CompilerAsserts.compilationConstant(argumentNodes.length);
    CompilerAsserts.compilationConstant(this.isTail());
    CompilerAsserts.compilationConstant(this.inPromise());

    if (argumentNodes.length > function.getCardinality()) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      throw new AbzuException("Unexpected number of arguments when calling '" + function.getName() +
          "': " + argumentNodes.length + " expected: " + function.getCardinality(), this);
    } else if (argumentNodes.length < function.getCardinality()) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      /*
       * Create a closure for partially applied function
       */
      String partiallyAppliedFunctionName = "$partial-" + argumentNodes.length + "/" + function.getCardinality() + "-" + function.getName();
      ExpressionNode[] allArgumentNodes = new ExpressionNode[function.getCardinality()];

      for (int i = 0; i < function.getCardinality(); i++) {
        allArgumentNodes[i] = new ReadArgumentNode(i);
      }

      /*
       * Partially applied function will just invoke the original function with arguments constructed as a combination
       * of those which were provided when this closure was created and those to be read on the following application
       */
      InvokeNode invokeNode = new InvokeNode(language, new SimpleIdentifierNode(function.getName()), allArgumentNodes);
      BlockNode blockNode = new BlockNode(new ExpressionNode[]{
          /*
           * We need to make sure that the original function is still accessible within the closure, even if the partially
           * applied function already leaves the scope with the original function
          */
          WriteLocalVariableNodeGen.create(functionNode, frame.getFrameDescriptor().findOrAddFrameSlot(function.getName())),
          invokeNode
      });

      FunctionNode partiallyAppliedFunctionNode = new FunctionNode(language, getSourceSection(), partiallyAppliedFunctionName,
          function.getCardinality() - argumentNodes.length + 1, frame.getFrameDescriptor(), blockNode);
      return partiallyAppliedFunctionNode.executeGeneric(frame);
    } else {
      Object[] argumentValues = new Object[argumentNodes.length];
      StringBuilder sb = new StringBuilder();
      boolean isPromise = false;
      for (int i = 0; i < argumentNodes.length; i++) {
        argumentValues[i] = argumentNodes[i].executeGeneric(frame);
        if (argumentValues[i] instanceof Promise) {
          Promise promise = (Promise) argumentValues[i];
          isPromise = true;
//          sb.append(promise.getId() + " _ ");
        } else {
//          sb.append(argumentValues[i].toString() + " _ ");
        }
      }

//      if (this.inPromise() != null) {
//        Promise argsPromise = Promise.all(argumentValues);
//        return argsPromise.mapUnwrap(fulfilledValues -> dispatchNode.executeDispatch(function, (Object[]) fulfilledValues));
//      }

//      if (isPromise) {
//        Promise argsPromise = Promise.all(argumentValues);
//        return argsPromise.mapUnwrap(fulfilledValues -> dispatchNode.executeDispatch(function, (Object[]) fulfilledValues));
//      }

      if (this.isTail()) {
        throw new TailCallException(function, argumentValues);
      }

      Function dispatchFunction = function;
      while (true) {
        try {
          return dispatchNode.executeDispatch(dispatchFunction, argumentValues);
        } catch (TailCallException e) {
          dispatchFunction = e.function;
          argumentValues = e.arguments;
        }
      }
    }
  }

  @Override
  public boolean hasTag(Class<? extends Tag> tag) {
    if (tag == StandardTags.CallTag.class) {
      return true;
    }
    return super.hasTag(tag);
  }
}
