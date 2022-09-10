REST API
========

### GET /changes/.../follow

Check if the given change can be updated by the follow-me plugin and return basic information about the change.

Returns a JSON object with the following properties:

* `on_review_branch`: boolean, whether the change is on the right branch. Used to show/hide the 'SELECT' button.
* `valid_review_target`: boolean, whether the change has a valid `Review-Target:` footer.
* `review_target`: string, the contents of the `Review-Target:` footer.
* `version`: string, the resolved version of `review_target`.
* `follow_branch`: string, the contents of the configuration key `plugins.followme.followBranch`.
* `follow_version`: string, the resolved version of `follow_branch`.
* `review_files`: multiline string, the contents of the `Review-Files:` footer, one line per footer line.

### POST /changes/.../follow

Update the change or determine what would be changed with an update.
Uses a JSON object as input and t
Creates a new patchset with the update if requested to do so.

The following input JSON object properties are supported:

* `do_update`: boolean, whether to create a new patchset or just simulate the changes.
* `new_review_target`: string, new value for the `Review-Target:` footer.
* `new_review_files`: multiline string, new values for the `Review-Files:` footer(s), one per line.

Returns a JSON object with the following properties:

* `on_review_branch`: boolean, whether the change is on the right branch.
* `valid_review_target`: boolean, whether the `new_review_target` is a valid Review-Target.
* `new_patchset_id`: integer, number of new patchset when `do_update` was requested.
* `version`: string, the resolved version of `new_review_target`.
* `added_paths`: array of strings, all file names which are newly added to the review by the new selection.
* `updated_paths`: array of strings, all file names which are updated by the `new_review_target` selection.
* `removed_paths`: array of strings, all file names which are no longer part of the review with by the new selection.

