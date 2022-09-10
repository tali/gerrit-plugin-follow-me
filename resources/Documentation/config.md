Configuration
=============

The configuration of the @PLUGIN@ plugin is done in the `gerrit.config`
file.

```
  [plugin "@PLUGIN@"]
    followBranch = refs/heads/main
    reviewBranch = refs/heads/review

```

plugin.@PLUGIN@.followBranch
:	The branch to follow.

	File content on this branch can be copied to new review patch sets.

	By default `refs/heads/master`.

plugin.@PLUGIN@.reviewBranch
:	The branch which is managed by this plugin.

	The branch is used to track the already reviewed content.
	New patch sets can be created for all changes on this branch.

	By default `refs/heads/review`.

plugin.@PLUGIN@.reviewTargetFooter
:	The name of the footer line which specifies the current target version.

	All file contents in the change are taken from the specified version,
	so that it is easy to verify what is already reviewed by looking
	at the change message.

	By default `Review-Target`.

plugin.@PLUGIN@.reviewFilesFooter
:	The name of the footer line which specifies the file matching patterns .

	All files in the change are taken based on the specified patterns
	so that it is easy to verify what is already reviewed by looking
	at the change message.

	By default `Review-Files`.

plugin.@PLUGIN@.versionPrefix
:	Prefix for git refs that are used to determine the version.

	References matching this `versionPrefix` will be used to determine
	the `Review-Target` footer which is added to changes when they are
	updated.

	By default `refs/tags/`.

plugin.@PLUGIN@.versionDropPrefix
:	Prefix from Git refs which is removed before the ref name is used as `version`.

	Can be different from `versionPrefix` so that a part of the required prefix is still visible.

	By default `refs/tags/`.

