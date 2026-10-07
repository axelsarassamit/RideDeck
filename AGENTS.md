# Release workflow requirement

After pushing changes to either RideDeck app, check the Android build workflow in BOTH axelsarassamit/RideDeck and axelsarassamit/RideDeck-for-Yamaha. Wait for the builds for the actual pushed commits to finish and inspect failed logs. Check the matching signing workflow when producing an installable release. Report each result accurately; a successful older commit does not verify newer local changes. If GitHub prevents uploads, state that the new source remains unverified by CI. Never claim a workflow passed while it is pending or running.
