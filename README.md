Gerrit follow-me plugin
=======================

`follow-me` is a Gerrit plugin to support managing one 'review' Branch,
which follows some other 'to-be-reviewed' branch by copying the contents of the
'to-be-reviewed' branch.

This allows a _post-commit_ review using Gerrit's workflow.


Use cases
---------

The workflow of creating an extra 'review' branch is helpful when the
'to-be-reviewed' branch already exists and has not yet been reviewed in the
requested quality.

While a _pre-commit_ review is generally preferable to a _post-commit_ workflow,
there are valid reasons for a review of an existing branch.

For example:

* Development was started without a review workflow,
  and mandatory reviews are requested later-on.
  It's easy to start reviews for new changes,
  but reviewing the exisiting codebase is much more ambitious.
* Especially for safety critical software, additional reviews by an
  independent department may be required.
  These reviews often concentrate on other aspects of the code,
  e.g. they do not have to judge whether the code is maintainable,
  but just have to confirm that the code is ready/safe for production.

Often the granularity of these new reviews does not align with the
original changes on the 'to-be-reviewed' branch.
Individual review of the original changes may be too fine-grained because it
would require to intermittent states which never made it into the final
version.
On the other hand, just importing a new version and reviewing the complete diff
as one change is not viable for any real codebase.

So we need a way to split these reviews into multiple smaller parts.
Depending on the codebase and the changes made, these smaller reviews can
concentrate on individual files, components or on special types of files.

The 'review' branch is used to document how much of the 'to-be-reviewed' branch
has already been reviewed.


Functionality
-------------

TBD TODO

### Create new review

* **TBD** show list of files which are not up-to-date in the 'review' branch
* **TBD** allow to select files for review
* **TBD** create new change containing the selected files

### Add new file content to existing changes

* **TBD** show when files in the review change have changed within the 'to-be-reviewed' branch
* **TBD** allow to create a new patchset with new versions
* **TBD** allow to select new files to add to the review

### Create merge commit to synchronize branches

* **TBD** create merge commit to selected tag


Example Workflows
-----------------

TBD

### Initial review

TBD

### Additional review

TBD
