package feature_extractor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.util.ArrayList;

import java.io.File;
import java.util.List;

/**
 * @Author: sxz
 * @Date: 2024/01/11/19:57
 * @Description:
 */
public class RefactoringManager {
    GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

    public List<Refactoring> detect(Repository repo, String commitId) {
        final List<Refactoring> refactorings = new ArrayList<>();
        miner.detectAtCommit(repo, commitId, new RefactoringHandler() {
            @Override
            public void handle(String commitId, List<Refactoring> detectedRefactorings) {
                refactorings.addAll(detectedRefactorings);
            }
        });
        return refactorings;
    }

    public List<Refactoring> detect(Repository repo, String commitId1, String commitId2) throws Exception {
        final List<Refactoring> refactorings = new ArrayList<>();
        miner.detectBetweenCommits(repo, commitId1, commitId2, new RefactoringHandler() {
            @Override
            public void handle(String commitId, List<Refactoring> detectedRefactorings) {
                refactorings.addAll(detectedRefactorings);
            }
        });
        return refactorings;
    }

}