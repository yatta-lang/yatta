package abzu.ast.expression;

import abzu.ast.ExpressionNode;
import abzu.runtime.Unit;
import abzu.runtime.async.AbzuFuture;
import abzu.runtime.async.Promise;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class PatternLetNode extends LexicalScopeNode {
  @Children
  public ExpressionNode[] patternAliases;  // PatternAliasNode | AliasNode
  @Child
  public ExpressionNode expression;

  public PatternLetNode(ExpressionNode[] patternAliases, ExpressionNode expression) {
    this.patternAliases = patternAliases;
    this.expression = expression;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PatternLetNode letNode = (PatternLetNode) o;
    return Arrays.equals(patternAliases, letNode.patternAliases) &&
        Objects.equals(expression, letNode.expression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(Arrays.hashCode(patternAliases), expression);
  }

  @Override
  public String toString() {
    return "PatternLetNode{" +
        "patterns=" + Arrays.toString(patternAliases) +
        ", expression=" + expression +
        '}';
  }

  @Override
  public void setIsTail(boolean isTail) {
    super.setIsTail(isTail);
    this.expression.setIsTail(isTail);
  }

  @Override
  public void setInPromise(Promise inPromise) {
    super.setInPromise(inPromise);
    this.expression.setInPromise(inPromise);
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    CompletableFuture firstFuture = null;
    MaterializedFrame materializedFrame = frame.materialize();

    for (int i = 0; i < patternAliases.length; i++) {
      Object aliasValue = patternAliases[i].executeGeneric(materializedFrame);
      if (aliasValue instanceof AbzuFuture) {
        AbzuFuture future = (AbzuFuture) aliasValue;
        if (firstFuture == null) {
          firstFuture = future.completableFuture;
        } else {
          firstFuture = firstFuture.thenCompose(ignore -> future.completableFuture);
        }
      }
    }

    if (firstFuture != null) {
      return new AbzuFuture(firstFuture);
    } else {
      return expression.executeGeneric(frame);
    }
  }
}
