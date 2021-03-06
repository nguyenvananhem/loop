package loop.lisp;

import loop.Token;
import loop.ast.BinaryOp;
import loop.ast.Call;
import loop.ast.CallArguments;
import loop.ast.Computation;
import loop.ast.InlineListDef;
import loop.ast.JavaLiteral;
import loop.ast.Node;
import loop.ast.TernaryIfExpression;
import loop.ast.Variable;
import loop.ast.script.ArgDeclList;
import loop.ast.script.FunctionDecl;
import loop.runtime.Closure;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
class SexprAstTransformer {
  private Node ast;

  public SexprAstTransformer(Node ast) {
    this.ast = ast;
  }

  public Node transform() {
    ast = reduceNode(ast);
    return reduce(ast);
  }

  private Node reduce(Node bloated) {
    List<Node> reduced = new ArrayList<Node>();

    for (Node child : bloated.children()) {
      reduced.add(reduceNode(child));
    }

    bloated.children().clear();
    bloated.children().addAll(reduced);

    return bloated;
  }

  private Node reduceNode(Node child) {
    // Not all parts of the sexpr are lists.
    if (child instanceof InlineListDef) {
      InlineListDef list = (InlineListDef) child;

      if (!list.children().isEmpty()) {
        Node first = list.children().get(0);
        if (first instanceof Variable) {
          Variable var = (Variable) first;

          if ("define".equals(var.name)) {
            return transformToFunctionDecl(list);
          } else {
            // This is a function call.
            return transformToFunctionCall(var, list);
          }
        } // Else this is just a real list(!)
      }
    }

    return child;
  }

  private Node transformToFunctionCall(Variable var, InlineListDef list) {
    // Special case binary ops?
    Token.Kind kind = Token.Kind.determine(var.name);
    switch(kind) {
      case PLUS:
      case MINUS:
      case DIVIDE:
      case MODULUS:
      case STAR:
      case ASSIGN:
      case GREATER:
      case LESSER:
      case GEQ:
      case LEQ:
        Computation computation = new Computation();
        computation.add(reduceNode(list.children().get(1)));

        // Add binary op chain.
        for (int i = 2; i < list.children().size(); i++) {
          BinaryOp op = new BinaryOp(new Token(var.name, kind, var.sourceLine, var.sourceColumn));
          op.add(reduceNode(list.children().get(i)));
          computation.add(op);
        }

        return computation;
    }

    TernaryIfExpression ifExpression = new TernaryIfExpression();
    Computation test = new Computation();
    test.add(var);
    test.add(new BinaryOp(new Token("in", Token.Kind.IN, var.sourceLine, var.sourceColumn))
        .add(new JavaLiteral('"' + Closure.class.getName() + '"')));
    ifExpression.add(test);                               // test := var instanceof closure
    ifExpression.add(toFunctionCall(var, list));          // then call
    ifExpression.add(var);                                // else return as is

    return ifExpression;
  }

  private Node toFunctionCall(Variable var, InlineListDef list) {
    CallArguments callArguments = new CallArguments(true);

    if (list.children().size() > 1) {
      List<Node> children = list.children();
      for (int i = 1, childrenSize = children.size(); i < childrenSize; i++) {
        callArguments.add(reduceNode(children.get(i)));
      }
    }

    return new Call(var.name, callArguments);
  }

  private FunctionDecl transformToFunctionDecl(InlineListDef list) {
    if (list.children().size() != 4)
      throw new RuntimeException("Incorrect number of arguments: " + list);

    Node second = list.children().get(1);
    String name;
    if (second instanceof Variable) {
      Variable var = (Variable) second;

      name = var.name;
    } else
      throw new RuntimeException("Expected function name in (define)");

    Node third = list.children().get(2);
    if (!(third instanceof InlineListDef))
      throw new RuntimeException("Expected argument definition");

    ArgDeclList args = new ArgDeclList();
    List<Node> exprs = reduce(third).children();
    for (Node expr : exprs) {
      Variable argName = (Variable) expr;
      args.add(new ArgDeclList.Argument(argName.name, null));
    }

    FunctionDecl functionDecl = new FunctionDecl(name, args);

    Node fourth = list.children().get(3);
    functionDecl.children().add(reduceNode(fourth));
    return functionDecl;
  }
}
