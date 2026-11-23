INSERT INTO time_boosts (target_id, source_id, epoch_millis, duration, boost_source)
VALUES (?, ?, ?, ?, ?)
ON CONFLICT(target_id, source_id) DO UPDATE SET
epoch_millis = excluded.epoch_millis,
duration = excluded.duration,
boost_source = excluded.boost_source;
