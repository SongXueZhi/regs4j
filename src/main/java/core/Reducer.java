package core;

import model.ChangedFile;
import model.Revision;
import model.TestFile;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.*;
import utils.CodeUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Reducer {

    public void reduceTestCases(Revision revision, Map<String,List<String>> testCaseMap) {
        Iterator<ChangedFile> iterator = revision.getChangedFiles().iterator();
        while(iterator.hasNext()) {
            ChangedFile file = iterator.next();
            if (file instanceof TestFile) {
                String testClass = judgeTestForBuggy(file.getNewPath(),testCaseMap.keySet());
                if (testClass == null){
                    iterator.remove();
                    continue;
                }else {
                    reduceTestCase((TestFile) file,testCaseMap.get(testClass),revision.getLocalCodeDir());
                }
            }
        }
    }
    private String judgeTestForBuggy(String path,Set<String> testClassSet){
        for (String testClass:testClassSet) {
            if (path.contains(testClass)){
                return testClass;
            }
        }
        return null;
    }
    private void reduceTestCase(TestFile testFile,List<String> testMethods,File rfcDir) {
        String path = testFile.getNewPath();
        File file = new File(rfcDir, path);
        try {
            CompilationUnit unit = CodeUtil.parseCompliationUnit(FileUtils.readFileToString(file,
                    "UTF-8"));
            List<TypeDeclaration> types = unit.types();
            for (TypeDeclaration type : types) {
                MethodDeclaration[] mdArray = type.getMethods();
                for (int i = 0; i < mdArray.length; i++) {
                    MethodDeclaration method = mdArray[i];
                    String name = method.getName().toString();
                    if ((method.toString().contains("@Test") || name.startsWith("test") || name.endsWith("test")) && !testMethods.contains(name)) {
                        method.delete();
                    }
                }
            }
            List<ImportDeclaration> imports = unit.imports();
            int len = imports.size();
            ImportDeclaration[] importDeclarations = new ImportDeclaration[len];
            for (int i = 0; i < len; i++) {
                importDeclarations[i] = imports.get(i);
            }

            for (ImportDeclaration importDeclaration : importDeclarations) {
                String importName = importDeclaration.getName().getFullyQualifiedName();
                if (importName.lastIndexOf(".") > -1) {
                    importName = importName.substring(importName.lastIndexOf(".") + 1);
                } else {
                    importName = importName;
                }

                boolean flag = false;
                for (TypeDeclaration type : types) {
                    if (type.toString().contains(importName)) {
                        flag = true;
                    }
                }
                if (!(flag || importDeclaration.toString().contains("*"))) {
                    importDeclaration.delete();
                }
            }
            FileUtils.forceDeleteOnExit(file);
            FileUtils.writeStringToFile(file, unit.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
