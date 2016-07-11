package co.otipc;

import co.otipc.job.Executor;
import co.otipc.job.Job;
import co.otipc.job.PhysicalPlain;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Chaoguo.Cui on 16/7/7.
 */
public class App {


  public static void main(String[] args) throws JSQLParserException, IOException {


    //    String sql =
    //      "SELECT MY_TABLE1.* FROM MY_TABLE1, MY_TABLE2, (SELECT * FROM MY_TABLE3) LEFT OUTER JOIN MY_TABLE4 "
    //        + " WHERE ID = (SELECT MAX(ID) FROM MY_TABLE5) AND ID2 IN (SELECT * FROM MY_TABLE6) AND id3=3";

    String sql = "select * from table_1 where age=31 and name='otipc'";

    PhysicalPlain physicalPlain = new PhysicalPlain();

    Job job = physicalPlain.getJob(sql);

    System.out.println(job);

    Executor executor = new Executor();

    List<String> result = executor.exec(job);

    for (String str : result) {
      System.out.println(str);
    }





    //    System.out.println(JSON.toJSONString(result));


  }

  private static void testxx(String sql) throws JSQLParserException {
    Statement parse = CCJSqlParserUtil.parse(sql);
    Select select = (Select) parse;
    PlainSelect ps = (PlainSelect) select.getSelectBody();




    Map<Object, Object> result = printPlan(ps);
    System.out.println(result);
  }

  private static Map<Object, Object> testExpr(Expression expression) {

    Map<Object, Object> result = new HashMap<>();

    if (expression instanceof AndExpression) {
      AndExpression and = (AndExpression) expression;
      Expression left = and.getLeftExpression();
      Expression right = and.getRightExpression();

      result.put(testExpr(left), testExpr(right));

    } else if (expression instanceof EqualsTo) {
      EqualsTo eq = (EqualsTo) expression;
      Expression left = eq.getLeftExpression();
      Expression right = eq.getRightExpression();
      //      result.put("where_eq_l", testExpr(left));
      //      result.put("where_eq_r", testExpr(right));
      result.put(testExpr(left), testExpr(right));
    } else if (expression instanceof SubSelect) {
      SubSelect subSelect = (SubSelect) expression;
      PlainSelect plain = (PlainSelect) subSelect.getSelectBody();
      result.put("subSelect", printPlan(plain));
    } else if (expression instanceof InExpression) {
      InExpression in = (InExpression) expression;
      Expression left = in.getLeftExpression();
      result.put("where_eq_l", testExpr(left));
      ItemsList items = in.getRightItemsList();
      if (items instanceof SubSelect) {
        SubSelect subSelect = (SubSelect) items;
        PlainSelect plain = (PlainSelect) subSelect.getSelectBody();
        result.put("subSelect", printPlan(plain));
      }
    } else if (expression instanceof Column) {
      result.put("Column", expression);
    } else if (expression instanceof Function) {
      Function fun = (Function) expression;
      result.put("function", fun.getName());
      ExpressionList list = fun.getParameters();
      for (int i = 0; i < list.getExpressions().size(); i++) {
        result.put("param_" + i, list.getExpressions().get(i).toString());
      }
    } else {
      result.put("xxx", expression);
    }


    return result;
  }

  private static Map<Object, Object> testItem(SelectItem item) {
    Map<Object, Object> result = new HashMap<>();

    if (item instanceof AllColumns) {
      result.put("all", "all");
    } else if (item instanceof AllTableColumns) {
      AllTableColumns columns = (AllTableColumns) item;
      result.put("all", "all");
      result.put("table", columns.getTable());
    } else if (item instanceof SelectExpressionItem) {
      Expression expression = ((SelectExpressionItem) item).getExpression();
      result.put("item", testExpr(expression));
    }



    return result;
  }

  private static Map<Object, Object> printPlan(PlainSelect plain) {

    Map<Object, Object> result = new HashMap<>();

    if (null != plain.getSelectItems()) {
      List<Object> l = new ArrayList<>();
      for (SelectItem item : plain.getSelectItems()) {
        l.add(testItem(item));
      }
      result.put("selectItem", l);
    }

    if (null != plain.getFromItem()) {
      result.put("from", plain.getFromItem());
    }
    if (null != plain.getWhere()) {
      Expression expression = plain.getWhere();
      result.put("where", testExpr(expression));
    }
    if (null != plain.getGroupByColumnReferences()) {
      result.put("group by", plain.getGroupByColumnReferences());
    }

    if (null != plain.getOrderByElements()) {
      result.put("order by", plain.getOrderByElements());
    }

    if (null != plain.getJoins()) {
      List<Object> joins = new ArrayList<>();
      for (int i = 0; i < plain.getJoins().size(); i++) {
        FromItem fromItem = plain.getJoins().get(i).getRightItem();
        if (fromItem instanceof SubSelect) {
          SubSelect subSelect = (SubSelect) fromItem;
          final PlainSelect subPs = (PlainSelect) subSelect.getSelectBody();
          if (null != subPs) {
            joins.add(printPlan(subPs));
          } else {
            joins.add(fromItem.toString());
          }
        } else {
          joins.add(fromItem.toString());
        }
      }
      result.put("joins", joins);
    }

    return result;



  }

  private static void test2() throws JSQLParserException {
    String sql =
      "select t1.a, t2.b from t1, t2, t3 where t1.id = t2.id and a > b group by name order by id";
    Statement parse = CCJSqlParserUtil.parse(sql);
    Select select = (Select) parse;
    PlainSelect ps = (PlainSelect) select.getSelectBody();
    System.out.println(ps);
    System.out.println(ps.getFromItem());
    System.out.println(ps.getJoins());

    System.out.println(ps.getWhere());

    System.out.println(ps.getGroupByColumnReferences());
    System.out.println(ps.getOrderByElements());
  }

  private static void test() throws JSQLParserException {

    CCJSqlParserManager pm = new CCJSqlParserManager();

    String sql = "select count(*) from table1";

    Statement stat = pm.parse(new StringReader(sql));

    if (stat instanceof Select) {
      Select select = (Select) stat;
      PlainSelect plainSelect = (PlainSelect) select.getSelectBody();

      System.out.println(plainSelect.getSelectItems().size());

      for (SelectItem item : plainSelect.getSelectItems()) {
        if (item instanceof SelectExpressionItem) {
          Expression expression = ((SelectExpressionItem) item).getExpression();
          if (expression instanceof Function) {
            Function function = (Function) expression;
            System.out.println(function.isAllColumns());
            System.out.println(function.getName());
          }
        }
      }


    }
  }



}
