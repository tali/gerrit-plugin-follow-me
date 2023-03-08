package com.googlesource.gerrit.plugins.reviewtarget;

import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.Change;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.ChangeInput;
import com.google.gerrit.lifecycle.LifecycleManager;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountManager;
import com.google.gerrit.server.account.AuthRequest;
import com.google.gerrit.server.change.RebaseUtil;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.notedb.ChangeNotes;
import com.google.gerrit.server.schema.SchemaCreator;
import com.google.gerrit.server.util.ThreadLocalRequestContext;
import com.google.gerrit.testing.InMemoryModule;
import com.google.gerrit.testing.InMemoryRepositoryManager;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class UpdateTreeTest {
  private Change change;
  private LifecycleManager lifecycle;
  private Repository repo;
  private IdentifiedUser user;

  @Inject private AccountManager accountManager;
  @Inject private AllProjectsName allProjects;
  @Inject private AuthRequest.Factory authRequestFactory;
  @Inject private ChangeNotes.Factory changeNotesFactory;
  @Inject private GerritApi gApi;
  @Inject private InMemoryRepositoryManager repoManager;
  @Inject private ThreadLocalRequestContext requestContext;
  @Inject private SchemaCreator schemaCreator;
  @Inject private IdentifiedUser.GenericFactory userFactory;

  @Inject private UpdateUtil updateUtil;
  @Inject private RebaseUtil rebaseUtil;
  private UpdateTree updateTree;

  public void setUpInjector() throws Exception {
    Injector injector = Guice.createInjector(new InMemoryModule());
    injector.injectMembers(this);
    lifecycle = new LifecycleManager();
    lifecycle.add(injector);
    lifecycle.start();

    schemaCreator.create();
    Account.Id userId = accountManager.authenticate(authRequestFactory.createForUser("user")).getAccountId();
    user = userFactory.create(userId);

    requestContext.setContext(() -> user);
  }

  private void setUpChange() throws Exception {
    ChangeInput input = new ChangeInput();
    input.project = allProjects.get();
    input.branch = "master";
    input.newBranch = true;
    input.subject = "Test change";
    ChangeInfo info = gApi.changes().create(input).get();
    ChangeNotes notes = changeNotesFactory.createChecked(allProjects, Change.id(info._number));
    change = notes.getChange();
    repo = repoManager.openRepository(allProjects);
  }

  @Before
  public void setUp() throws Exception {
    setUpInjector();
    setUpChange();

    updateTree = new UpdateTree(repo, change, updateUtil, rebaseUtil);
  }

  @After
  public void tearDown() {
    if (lifecycle != null) {
      lifecycle.stop();
    }
    requestContext.setContext(null);
  }

  @Test
  public void isPathToBeReviewed_1() {
    updateTree.newReviewFiles("src");
    assertThat(updateTree.isPathToBeReviewed("test", true))
        .isEqualTo(UpdateTree.Selected.NO_MATCH);
    assertThat(updateTree.isPathToBeReviewed("src", true))
        .isEqualTo(UpdateTree.Selected.POSITIVE);
    assertThat(updateTree.isPathToBeReviewed("component/src", true))
        .isEqualTo(UpdateTree.Selected.POSITIVE);
    assertThat(updateTree.isPathToBeReviewed("component/test", true))
        .isEqualTo(UpdateTree.Selected.NO_MATCH);
    assertThat(updateTree.isPathToBeReviewed("src", false))
        .isEqualTo(UpdateTree.Selected.POSITIVE);
    assertThat(updateTree.isPathToBeReviewed("src1", false))
        .isEqualTo(UpdateTree.Selected.NO_MATCH);
    assertThat(updateTree.isPathToBeReviewed("file.src", false))
        .isEqualTo(UpdateTree.Selected.NO_MATCH);
  }

  @Test
  public void isPathToBeReviewed_2() {
    updateTree.newReviewFiles("a.*\n!*.b");
    assertThat(updateTree.isPathToBeReviewed("x.x", false))
        .isEqualTo(UpdateTree.Selected.NO_MATCH);
    assertThat(updateTree.isPathToBeReviewed("a.x", false))
        .isEqualTo(UpdateTree.Selected.POSITIVE);
    assertThat(updateTree.isPathToBeReviewed("a.b", false))
        .isEqualTo(UpdateTree.Selected.NEGATIVE);
    assertThat(updateTree.isPathToBeReviewed("x.b", false))
        .isEqualTo(UpdateTree.Selected.NEGATIVE);
  }
}
