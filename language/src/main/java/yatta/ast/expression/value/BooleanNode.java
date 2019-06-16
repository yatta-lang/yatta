package yatta.ast.expression.value;

import yatta.ast.ExpressionNode;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

@NodeInfo
public final class BooleanNode extends ExpressionNode {
  public static BooleanNode TRUE = new BooleanNode(Boolean.TRUE);
  public static BooleanNode FALSE = new BooleanNode(Boolean.FALSE);

  public final boolean value;

  BooleanNode(boolean value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "BooleanNode{" +
        "value=" + value +
        '}';
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return value;
  }

  @Override
  public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
    return value;
  }
}