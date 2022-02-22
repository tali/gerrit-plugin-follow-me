Configuration
=============

The configuration of the @PLUGIN@ plugin is done in the `gerrit.config`
file.

```
  [plugin "@PLUGIN@"]
    allowDeletionOfReposWithTags = true

```

plugin.@PLUGIN@.followBranch
:	The branch to follow.

	TBD

	By default `refs/heads/master`.

plugin.@PLUGIN@.reviewBranch
:	The branch which is updated by this plugin.

	TBD

	By default `refs/heads/review`.

plugin.@PLUGIN@.versionFooter
:	The name of the footer line which receives the current version.

	TBD

	By default `Reviewed`.

plugin.@PLUGIN@.versionPrefix
:	Prefix for git refs that are used to determine the version.

	TBD

	By default `refs/tags/`.

