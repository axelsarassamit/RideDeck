# Release workflow requirement

After pushing changes to either RideDeck app, check the Android build workflow in BOTH axelsarassamit/RideDeck and axelsarassamit/RideDeck-for-Yamaha. Wait for the builds for the actual pushed commits to finish and inspect failed logs. Check the matching signing workflow when producing an installable release. Report each result accurately; a successful older commit does not verify newer local changes. If GitHub prevents uploads, state that the new source remains unverified by CI. Never claim a workflow passed while it is pending or running.

## GitHub plugin edit workflow

For related source, version, test, and documentation edits, prepare the complete change before advancing the main branch. Create all blobs, build one Git tree from the current main tree, create one commit with the current main commit as its parent, then advance main with an expected-head check. Do not push interdependent files through a sequence of Contents API commits to main, because each partial commit starts CI before the set is complete.

If a complete atomic commit cannot be made, use an isolated branch and merge only after the entire branch passes its required checks. Never hide, cancel, or rerun a failed check just to produce a green result. Inspect each failed log, fix the cause in a new complete commit, and wait for the exact resulting commit's Android build and tests to finish in both RideDeck repositories. For installable releases, sign only the exact source commit whose build passed.
