# Backup is automatic for the database and manual for the media

## Context

All data lives on the device, so losing the phone means losing everything unless the user acts. There
is no server and there will not be one, so backup has to cost nothing to run.

Android Auto Backup covers this for free — but with two properties that shape the whole design:

- **A per-app quota of about 25 MB**, and **Android rejects the entire over-quota dataset.** It does
  not back up partially. So a pile of photos does not cost "just the photos": it silently takes the
  database down with it, and the user believes they have a backup when they have none.
- **It runs unattended**, on the OS's schedule, with no UI and nobody present. Nothing can be
  surfaced at backup time.

And one that is easy to miss: **Room runs in WAL mode**, so the most recent committed writes may live
only in the `-wal` sidecar. Copying the `.db` alone hands over a file missing the very data the user
asked to keep — and copying the sidecar alongside it is the other half of the same trap, because a
`-wal` captured mid-write restores *corrupt*.

## Decision

**Two levels.**

**Automatic**, through a custom `BackupAgent`: the database, checkpointed, plus the small media. It
is an agent rather than `backup_rules.xml` because **static include/exclude XML cannot checkpoint the
WAL**, and there is no way to express "flush first" in it. Registered with `fullBackupOnly="true"` —
declaring an agent without it puts the app on the key/value path, where the no-op `onBackup` is what
runs and the app silently backs up nothing.

**Manual**, in the app: a single zip the user keeps wherever they like, at a chosen scope. Records
only (small enough to mail yourself) or everything (which can be hundreds of megabytes).

**The archive carries a manifest**, read first on restore. Without one, the only way to find out an
archive is from a newer version of the app is to open its database and have Room throw — by which
point the live file has already been replaced.

**A restore extracts into a staging directory and swaps in only once the extraction has completed.**
Extracting straight over the live files — which is where they are going anyway — turns every
interrupted restore into total data loss.

## Alternatives

**Include photos in Auto Backup.** Trades a reliable small backup for an unreliable large one, and
the failure is silent.

**`backup_rules.xml` and no agent.** Cannot checkpoint. Produces backups that appear to work.

**Restore in place.** See above. A user restoring a backup is already having a bad day.

**A cloud sync of our own.** A server, a bill, a privacy policy with something in it, and an account
system — for a feature the platform already provides.

## Consequences

- The export is written to a `.part` and renamed on success, so an interrupted export leaves no file
  that looks shareable and is not.
- A restore **deletes the `-wal` and `-shm` sidecars** before copying the new database in. Leaving a
  log belonging to a database that no longer exists is a corrupt open on next launch.
- The restorer guards against **zip slip** — an entry named `../../databases/other.db`. The archive
  arrives from the user's storage, not from the app, so it is never trusted even though the app wrote
  the format.
- Media is merged rather than replaced on restore: filenames are UUIDs, so a collision means the same
  file, and keeping files the archive does not mention costs disk where deleting them costs photos.

Amendment, 2026-08-29: superseded in practice by ADR-0007. Both halves are gone. There is no
database to checkpoint, so the custom AppBackupAgent is removed and the platform's default Auto
Backup covers the preferences file; and with no media there is nothing for a manual export to carry.
The reasoning above is kept intact rather than rewritten, because it is still correct for the app it
was written about, and it is what comes back if a Gloam feature ever calls for it.

Amendment, 2026-09-17 (Phase 5, R4): what Auto Backup actually does with the preferences file, read
off a device rather than asserted. Through `LocalTransport`, `pm clear` and `bmgr restore`, the file
came back byte for byte, and `lent_location.preferences_pb` stayed behind — ADR-0013 §8's exclusion
works. Neither the restore nor the first launch raised anything.

**What the reading changed is `shade_running`.** It is *live state* that travels in a file of
settings, and `phase-5.md` §9 predicted it would be refused "twice over": no overlay permission, and
a deadline that passed while the phone was off. Only the first refusal held. With auto-off at
*Never* there is no deadline to pass, so once the user grants the overlay on the new phone, the next
reboot or app update put the shade up at the old phone's dim level — measured, with no notification,
because nothing had asked for that permission yet. On HyperOS it is sooner than that: the ROM queues
`BOOT_COMPLETED` for an app in the stopped state and delivers it at the first launch.

So the intent now carries **`shade_began_at`**, written in `beginShade`'s transaction beside
`shade_running` and the deadline, and `BootReceiver` refuses an intent that is older than this
install's `firstInstallTime` — a value the platform never backs up. `RestoredIntentTest` holds the
comparison, including the two cases a phone cannot stage: an absent stamp, and the boundary
millisecond. An absent stamp is refused, so anybody updating with the shade up loses it once.

**The rule this leaves standing is still the one in `CLAUDE.md`**: the stored intent is what the user
asked for, and it is judged at the read. This is one more refusal in the place the other two already
live, not a manifest attribute and not a second store — `data_extraction_rules.xml` excludes files,
and every setting shares one.
