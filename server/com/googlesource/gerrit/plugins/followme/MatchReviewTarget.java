package com.googlesource.gerrit.plugins.followme;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.gerrit.entities.Change;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;

import static java.util.concurrent.TimeUnit.MINUTES;

@Singleton
public class MatchReviewTarget {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private GitRepositoryManager gitManager;
  private UpdateUtil updateUtil;
  private Configuration cfg;

  @Inject
  MatchReviewTarget(
      GitRepositoryManager gitManager,
      UpdateUtil updateUtil,
      Configuration cfg
  ) {
    this.gitManager = gitManager;
    this.updateUtil = updateUtil;
    this.cfg = cfg;
  }

  boolean checkReviewTarget(Change change) {

    try (
        Repository repo = gitManager.openRepository(change.getProject());
        UpdateTree update = new UpdateTree(repo, change, updateUtil);
    ) {
      update.useReviewTargetFooter(cfg.getReviewTargetFooter());
      update.useReviewFilesFooter(cfg.getReviewFilesFooter());

      if (!update.isValidReviewTarget()) {
        return false;
      }

      update.rewritePaths();
      return update.hasCurrentPaths();

    } catch (StorageException | UncheckedExecutionException | IOException e) {
      warnWithOccasionalStackTrace(
          e,
          "failure checking Review-Target footer for change %s: %s",
          change.getId(),
          e.getMessage());
      return false;
    }
  }

  private static void warnWithOccasionalStackTrace(Throwable cause, String format, Object... args) {
    logger.atWarning().logVarargs(format, args);
    logger
        .atWarning()
        .withCause(cause)
        .atMostEvery(1, MINUTES)
        .logVarargs("(Re-logging with stack trace) " + format, args);
  }

}
