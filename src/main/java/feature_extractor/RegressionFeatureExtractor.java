package feature_extractor;

import core.git.GitUtils;
import core.git.RepositoryProvider;
import model.Regression;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.Refactoring;

import java.io.File;
import java.util.List;

/**
 * @Author: sxz
 * @Date: 2024/01/11/19:51
 * @Description:
 */
public class RegressionFeatureExtractor {
    RefactoringManager refactoringManager =new RefactoringManager();
    public  void  detectRefactorInCommit(String projectDir, String commitID){
        try(Repository repository = RepositoryProvider.getRepoFromLocal(new File(projectDir));) {
            List<Refactoring> refactorings = refactoringManager.detect(repository,commitID);
            for (Refactoring refactoring: refactorings){
                refactoring.rightSide().stream().forEach(codeRange->{
                    System.out.println(codeRange.getStartLine()+"->"+codeRange.getEndLine());
                });
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
