Gerrit reviewtarget plugin
==========================

`reviewtarget` is a Gerrit plugin to support managing one 'review' Branch,
which follows some other 'to-be-reviewed' branch by copying the contents of the
'to-be-reviewed' branch.

This allows a _post-commit_ review using Gerrit's workflow.


Functionality
-------------

This plugin can be used to create new Patch Sets following the contents of
another branch or version.

Already reviewed parts are submitted to the 'review' branch so that the
review progress is always tracked for all files.

### Rule-based selection of files

Instead of manually selecting individual files, rules can be used to automatically
select all matching files.
The Git `.gitignore` mechanism is used.

The rules for selecting files are stored in footer lines within the commit message.
This makes it easy to update the review with the same set of files targeting a new
version which is to-be-reviewed.

### Preview during creation of review rules

A preview for new review selections is shown so that it is easier to pick
the right rules for the file selection.

### Create new Patchsets based on a specified selection

Based on the selected rules for file selection, a new Patchset can be created
which contains all the matching files from the review target.

When creating a new patchset, both the Review-Files as well as the
Review-Target selection can be changed.

### Verify that a change adheres to the review selection

TBD NOT IMPLEMENTED

The rules for selecting files are stored in footer lines within the commit message.
This makes it easy to check that the all relevant files are part of the review commit,
without having to list all potential files by hand.

When the rule is correct then the user can be sure that any matching files are
part of the review.
In order to guarantee this property, a submit requirement can be configured to
prevent the change from being submitted when the footer and the file contents
do not match.

### Create merge commit to synchronize branches

TBD NOT IMPLEMENTED

* **TBD** create merge commit to selected tag

### Reparent instead of rebase

TBD NOT IMPLEMENTED

* **TBD** allow to change the parent of the change without changing the target tree


Example Workflows
-----------------

The base workfow in all scenarios is about the same:

* Create new empty change on 'review' branch
* Click the 'select' button, choose a subset of files which are suitable for review
* Click the 'update' button, a new Patchset is created which contains the selected files
* Review files
* Rework on 'to-be-reviewed' branch
* Click the 'select' button, choose new version
* Click the 'update' button to create a new Patchset with the reworked code
* Repeat review until the selected subset of the code meets the acceptance criteria
* Submit the change to the 'review' branch
* Repeat process with other subsets of files until the complete code is reviewed

When these steps are repeated, then the 'review' branch will follow the
'to-be-reviewed' branch.

This process can be used in different ways, by selecting Review-Target and
Review-Files accordingly.

### Review already released versions

One change is created per version, this version is selected as Review-Target.
When necessary, multiple changes with different Review-Files can be created
for each version.

As review findings cannot be corrected within the already released versions,
follow-up tickets should be created and referenced in the change message.
Gerrit can still be used to document and discuss all findings.
Once the follow-up ticket is closed, the Gerrit change can be submitted, too.

### Review of externally managed code

When code is managed in a legacy system (e.g. SVN, ClearCase) then pre-commit
reviews are not easily possible.
Usually commits are made directly on a development branch and cannot be
amended later.

When the code is imported to a Git branch, then this branch can be used
as Review-Target for Gerrit reviews.

Whenever there are new changes to some part of the 'to-be-review' branch,
a new Gerrit change is created to review these changes.
The Review-Files specification can be used to review a relevant subset of
all the changes made in the external subset.

Review findings are directly reworked in the external system and then
imported to the 'to-be-reviewed' branch in Gerrit.
They can be incorporated as new Patch Set by updating the Review-Target
to the new version.

Once this change stabilizes and there are no more review findings the
Gerrit change is submitted.

Note that this process works best when it is possible to coordinate
develoment in the external system with the review process,
so that no new unrelated changes are introduced to the files which
are currently being reviewed.
