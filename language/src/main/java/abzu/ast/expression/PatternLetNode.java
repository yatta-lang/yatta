package abzu.ast.expression;

import abzu.ast.ExpressionNode;
import com.oracle.truffle.api.frame.VirtualFrame;

import java.util.Arrays;
import java.util.Objects;

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
  public Object executeGeneric(VirtualFrame frame) {
    for (ExpressionNode patternAlias : patternAliases) {
      patternAlias.executeGeneric(frame);
    }

    return expression.executeGeneric(frame);
  }
}
