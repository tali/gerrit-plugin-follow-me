package com.googlesource.gerrit.plugins.reviewtarget;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.gerrit.entities.Change;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.server.change.RebaseUtil;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MINUTES;

@Singleton
public class MatchReviewTarget {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final GitRepositoryManager gitManager;
  private final UpdateUtil updateUtil;
  private final RebaseUtil rebaseUtil;
  private final Configuration cfg;

  @Inject
  MatchReviewTarget(
      GitRepositoryManager gitManager,
      UpdateUtil updateUtil,
      RebaseUtil rebaseUtil,
      Configuration cfg
  ) {
    this.gitManager = requireNonNull(gitManager);
    this.updateUtil = requireNonNull(updateUtil);
    this.rebaseUtil = requireNonNull(rebaseUtil);
    this.cfg = requireNonNull(cfg);
  }

  boolean checkReviewTarget(Change change) {

    try (
        Repository repo = gitManager.openRepository(change.getProject());
        UpdateTree update = new UpdateTree(repo, updateUtil, rebaseUtil);
    ) {
      update.useChange(change);

      if (!update.isValidReviewTarget()) {
        return false;
      }

      update.rewritePaths();
      return update.hasCurrentPaths();

    } catch (RestApiException | StorageException | UncheckedExecutionException | IOException e) {
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
