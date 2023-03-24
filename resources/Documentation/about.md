
`reviewtarget` is a Gerrit plugin to support managing one 'review' Branch,
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
* A branch is managed by an external system without review
  and imported into Gerrit for review.
* Especially for safety critical software, additional reviews by an
  independent validation department may be required.
  These reviews often concentrate on other aspects of the code,
  e.g. they do not have to judge whether the code is maintainable,
  but just have to confirm that the code is safe for production.

Often the granularity of these new reviews does not align with the
original changes on the 'to-be-reviewed' branch.
Individual review of the original changes may be too fine-grained because it
would require to review intermittent states which never made it into the final
version.
On the other hand, just importing a new version and reviewing the complete diff
as one change may not be viable.

So we need a way to split these reviews into multiple smaller parts.
Depending on the codebase and the changes made, these smaller reviews can
concentrate on individual files, components or on special types of files.

The 'review' branch is used to document how much of the 'to-be-reviewed' branch
has already been reviewed.


Challenges
----------

* be able to review a whole range of to-be-reviewed commits as one review change
* be able to review just a subset of files in each review change
* have to keep track which version has been reviewed for each file


Detailed Design
---------------

### Commit message footer

#### `Review-Files:`

Filter for files to review.
All matching files will be part of the review, i.e. will be copied from to-be-reviewed branch.

Same syntax as `.gitignore` lines.
Multiple entries can be combined by repeating the `Review-Files:` footer.
When no Reviev-Files filter is given, then the complete contents will be included in the review.

Examples:

* `Review-Files: *.c`
  `Review-Files: *.h`
* `Review-Files: src/common`
  `Review-Files: src/components`

#### `Review-Target:`

Names the version on the to-be-reviewed branch which is being reviewed.
Can either be a branch name or a tag name.

Examples:

* `Review-Target: v3.6.0`
* `Review-Target: svn/trunk@1234`


### Submit Requirement

This plugin provides a search operand for changes which are managed by the plugin.

* `has:selected_reviewtarget`: all changes with a `Review-Target:` footer where the contents match the footer specification.
That can be used to configure a submit requirement, so that only unmodified changes can be submitted.

Example:

```
[submit-requirement "Review-Target"]
description = The review target has to be specified by clicking on the SELECT button.
applicableIf = branch:review
submittableIf = has:selected_reviewtarget
```
