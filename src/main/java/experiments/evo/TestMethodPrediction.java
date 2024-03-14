package experiments.evo;

import org.apache.commons.io.FileUtils;
import spoon.Launcher;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestMethodPrediction {
    int errorLine = 0;
    List<Methodx> testMethodList = new ArrayList<>();
    Set<Variablex> ctVariableArrayList = new HashSet<>();

    private void addToTestedMethodList(CtInvocation<?> invocation) {
        //被测方法不能是构造方法
        CtExecutableReference<?> executable = invocation.getExecutable();
        if (executable == null || executable.isConstructor()) {
            return;
        }
        //被测方法不能是getter/setter方法
        if (judgeGetterSetterMethod(executable)) {
            return;
        }

        //被测方法不能是JUnit、TestNG、Hamcrest等测试框架的方法
        String declaringTypeQualifiedName = executable.getDeclaringType().getQualifiedName();
        if (declaringTypeQualifiedName.startsWith("org.junit") ||
                declaringTypeQualifiedName.startsWith("org.testng") ||
                declaringTypeQualifiedName.startsWith("org.hamcrest")) {
            return;
        }

        // 被测方法不能是测试包下的方法
        if (declaringTypeQualifiedName.contains(".test.") ||
                declaringTypeQualifiedName.contains(".tests.")) {
            return;
        }

        testMethodList.add(new Methodx(declaringTypeQualifiedName, executable.getSimpleName()));
    }


    // 检查是否为getter或setter命名
    private boolean hasGetterSetterPrefix(String methodName) {
        return methodName.matches("^(get|set|is|has|can|should).*");
    }

    private boolean isGetterSetterMethod(CtExecutable<?> method) {
        if (method == null) {
            return false;
        }
        //setter 允许有多个赋值语句
        List<CtAssignment<?,?>> assignments = method.getElements(new TypeFilter<>(CtAssignment.class));
        boolean allAccessField = !assignments.isEmpty() && assignments.stream().allMatch(assignment -> {
            boolean isFieldWrite = assignment.getAssigned() instanceof CtFieldWrite;
            boolean isSimpleAssignment = assignment.getAssignment() instanceof CtLiteral ||
                    assignment.getAssignment() instanceof CtVariableRead;
            return isFieldWrite && isSimpleAssignment;
        });

        //getter 所有返回语句都是字段访问
        List<CtReturn<?>> returns = method.getElements(new TypeFilter<>(CtReturn.class));
        boolean allMatchReturnField = !returns.isEmpty() && returns.stream().allMatch(returnStatement -> {
            CtExpression<?> returnedExpression = ((CtReturn<?>) returnStatement).getReturnedExpression();
            return returnedExpression instanceof CtFieldRead;
        });

        List<CtStatement> statements = method.getBody().getStatements();
        //所有的statements要么是赋值语句，要么是返回语句
        boolean allAssignmentOrReturn = statements!=null && statements.stream().allMatch(statement -> statement instanceof CtAssignment || statement instanceof CtReturn);
        boolean lookAsDataMethod = allAccessField || allMatchReturnField;
        return allAssignmentOrReturn ? lookAsDataMethod
                : method.getBody().getStatements().size()<4 && lookAsDataMethod;
    }

    private boolean judgeGetterSetterMethod(CtExecutableReference<?> executable) {
       CtExecutable<?> declaration = executable.getDeclaration();

       if (!hasGetterSetterPrefix(executable.getSimpleName())) {
         return false;
       }

       if (declaration.getBody() == null) {
         return false;
       }

        List<CtStatement> statements =  declaration.getBody().getStatements();
        if (statements.size() == 1 && statements.get(0) instanceof CtInvocation) {
            CtInvocation<?> innerInv = (CtInvocation<?>) statements.get(0);
            return isGetterSetterMethod(innerInv.getExecutable().getDeclaration());
        } else {
            return isGetterSetterMethod(declaration);
        }

    }


    private void addVariableToList(Variablex variable) {
        String qualifiedName = variable.getQualifiedName();
        if (qualifiedName.startsWith("org.junit") ||
                qualifiedName.startsWith("org.testng") ||
                qualifiedName.startsWith("org.hamcrest")) {
            return;
        }

        // 被测方法不能是测试包下的方法
        if (qualifiedName.contains(".test.") ||
                qualifiedName.contains(".tests.")) {
            return;
        }

        ctVariableArrayList.add(variable);
    }

    /**
     * 解析断言中的变量，同时如果变量是通过方法调用得到的，则该方法可能是被测方法
     * @param invocation
     */
    public void parserAssertVariable(CtInvocation<?> invocation) {
        invocation.getArguments().stream().forEach(arg -> {
            if (arg instanceof CtInvocation) {
                CtInvocation<?> innerInvocation = (CtInvocation<?>) arg;
                CtExpression<?> target = innerInvocation.getTarget();
                if (target != null) {
                    CtTypeReference<?> type = target.getType();
                    addVariableToList(new Variablex(type.getQualifiedName(), target.toString()));
                }
                addToTestedMethodList(innerInvocation); //如果断言中有方法调用，则该方法可能是被测方法
            } else if (arg instanceof CtVariableRead) {
                CtVariableRead<?> variableRead = (CtVariableRead<?>) arg;
                CtTypeReference<?> type = variableRead.getType();
                addVariableToList(new Variablex(type.getQualifiedName(), variableRead.toString()));
            }
        });
    }

    /**
     *
     * @param method
     * @param errorLine
     * @return
     */
    public CtInvocation<?> getAssertInvocation(CtMethod<?> method, int errorLine) {
        return method.getElements(new TypeFilter<>(CtInvocation.class)).stream().filter(
                invocation -> invocation.getPosition().getLine() <= errorLine &&
                        invocation.getPosition().getEndLine() >= errorLine
        ).findFirst().orElse(null);
    }

    /**
     * 倒序遍历所有的statements，找到被测方法
     * @param method
     */
    public void processStatements(CtMethod<?> method) {
        //倒序遍历statements
        CtBlock<?> body = method.getBody();
        List<CtStatement> statements = body.getStatements();
        Collections.reverse(statements); // 倒序遍历语句
        statements.forEach(statement -> {
            //不需要再解析错误行之后的语句
            if (statement.getPosition().getLine() >= errorLine) {
                return;
            }
            if (statement instanceof CtAssignment) {
                CtAssignment<?, ?> assignment = (CtAssignment<?, ?>) statement;
                CtExpression<?> leftHandSide = assignment.getAssigned();
                CtExpression<?> rightHandSide = assignment.getAssignment();
                if (leftHandSide instanceof CtVariableWrite) {
                    String varName = ((CtVariableWrite<?>) leftHandSide).getVariable().getSimpleName();
                    String varType = ((CtVariableWrite<?>) leftHandSide).getVariable().getType().getQualifiedName();
                    //如果被测变量在赋值语句中出现
                    if (ctVariableArrayList.stream().anyMatch(variablex -> variablex.getVariableName().equals(varName)
                            && variablex.getQualifiedName().equals(varType))) {
                        // 如果赋值语句右边是方法调用，则该方法可能是被测方法
                        if (rightHandSide instanceof CtInvocation) {
                            CtInvocation<?> invocation = (CtInvocation<?>) rightHandSide;
                            addToTestedMethodList(invocation);

                            if (invocation.getTarget() != null) {
                                //调用被测方法的变量也被视为被测对象
                                CtTypeReference<?> type = invocation.getTarget().getType();
                                addVariableToList(new Variablex(type.getQualifiedName(), invocation.getTarget().toString()));
                            }
                        }
                        // 如果赋值语句右边只是一个变量访问，则该变量可能是被测对象
                        else if (rightHandSide instanceof CtVariableAccess) {
                            CtVariableAccess<?> variableAccess = (CtVariableAccess<?>) rightHandSide;
                            CtTypeReference<?> type = variableAccess.getType();
                            addVariableToList(new Variablex(type.getQualifiedName(), variableAccess.toString()));
                        }
                    }
                }
            } else if (statement instanceof CtInvocation) {
                CtInvocation<?> invocation = (CtInvocation<?>) statement;
                addToTestedMethodList(invocation);
            }
        });
    }

    public List<Methodx> predictTestMethod(CtMethod<?> method, int errorLine) {
        testMethodList.clear();
        ctVariableArrayList.clear();
        this.errorLine = errorLine;
        CtBlock<?> body = method.getBody();
        if (body == null) {
            return testMethodList;
        }
        CtInvocation<?> assertInvocation = getAssertInvocation(method, errorLine);
        if (assertInvocation != null) {
            parserAssertVariable(assertInvocation);
        }
        processStatements(method);
        return testMethodList;
    }
    //
    public static void main(String[] args) throws IOException {
        String errorMessage = FileUtils.readFileToString(new File(
                "/Users/sxz/reg4j/info/exceptions/2_ric.txt"
        ));
        //获取最后一个at开头的行
        ErrorMessageParser errorMessageParser = new ErrorMessageParser();
        String[] errorInfo = errorMessageParser.parseErrorMessage(errorMessage);

        // 给定的包名、类名和方法名
        String packageName = errorInfo[0];
        String className = errorInfo[1];
        String methodName = errorInfo[2];
        int errorLine = Integer.parseInt(errorInfo[3]);

        Launcher launcher = new Launcher();
        launcher.addInputResource("/Users/sxz/reg4j/cache_code/2_ric/src");
        launcher.buildModel();


        // 查找特定类和方法
        CtClass<?> clazz = launcher.getFactory().Package().get(packageName).getType(className);
        CtMethod<?> method = clazz.getMethodsByName(methodName).get(0);
        TestMethodPrediction testMethodPrediction = new TestMethodPrediction();
        testMethodPrediction.predictTestMethod(method, errorLine).forEach(System.out::println);

    }


}
