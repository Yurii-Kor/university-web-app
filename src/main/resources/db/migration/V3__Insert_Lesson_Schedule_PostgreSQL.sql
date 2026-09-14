DO $$
DECLARE
    schedule_timezone text := 'Asia/Jerusalem';

    weeks_back    int := 8;
    weeks_forward int := 8;

    first_week date;
    last_week  date;

    inserted_count bigint;
BEGIN
    ----------------------------------------------------------------------
    -- The schedule is anchored to the date on which this migration runs.
    --
    -- Example:
    --   migration executed during week 2026-09-14
    --   -> schedule starts 8 weeks before that week
    --   -> schedule ends 8 weeks after that week
    --
    -- Flyway executes a versioned migration only once, so these dates
    -- become stable after V3 has been applied.
    ----------------------------------------------------------------------

    first_week :=
            date_trunc(
                    'week',
                    CURRENT_DATE)::date
            - (weeks_back * 7);

    last_week :=
            date_trunc(
                    'week',
                    CURRENT_DATE)::date
            + (weeks_forward * 7);

    ----------------------------------------------------------------------
    -- Build a deterministic weekly timetable from the current
    -- group_courses configuration.
    --
    -- Each group-course relation receives two lessons per week.
    --
    -- There are six available lesson slots per weekday and five
    -- weekdays:
    --
    --   0 -> 08:00-09:30
    --   1 -> 09:45-11:15
    --   2 -> 11:30-13:00
    --   3 -> 13:15-14:45
    --   4 -> 15:00-16:30
    --   5 -> 16:45-18:15
    --
    -- Therefore one week provides 30 unique slots.
    --
    -- Every generated lesson receives a unique global weekly slot.
    -- This guarantees that generated lessons cannot overlap by:
    --
    --   * study group
    --   * teacher
    --   * room
    ----------------------------------------------------------------------

    WITH RECURSIVE

    week_starts AS (
        SELECT generate_series(
                first_week::timestamp,
                last_week::timestamp,
                interval '1 week'
        )::date AS week_start
    ),

    course_groups AS (
        SELECT
            gc.group_id,
            gc.course_id,
            c.teacher_id,
            c.code AS course_code,
            c.name AS course_name,
            g.name AS group_name
        FROM group_courses gc
        JOIN courses c
          ON c.id = gc.course_id
        JOIN groups g
          ON g.id = gc.group_id
        WHERE c.teacher_id IS NOT NULL
          AND g.deleted_at IS NULL
    ),

    lesson_patterns AS (
        SELECT
            cg.group_id,
            cg.course_id,
            cg.teacher_id,
            cg.course_code,
            cg.course_name,
            cg.group_name,
            occurrence.number AS occurrence_number,

            row_number() OVER (
                ORDER BY
                    cg.group_id,
                    cg.course_id,
                    occurrence.number
            ) - 1 AS global_slot
        FROM course_groups cg
        CROSS JOIN (
            VALUES
                (1),
                (2)
        ) AS occurrence(number)
    ),

    scheduled_patterns AS (
        SELECT
            lp.*,

            floor(lp.global_slot / 6)::int AS weekday_offset,

            (lp.global_slot % 6)::int AS daily_slot
        FROM lesson_patterns lp
    ),

    generated_lessons AS (
        SELECT
            sp.group_id,
            sp.course_id,
            sp.teacher_id,

            (
                (
                    ws.week_start
                    + sp.weekday_offset
                )::date
                +
                CASE sp.daily_slot
                    WHEN 0 THEN time '08:00'
                    WHEN 1 THEN time '09:45'
                    WHEN 2 THEN time '11:30'
                    WHEN 3 THEN time '13:15'
                    WHEN 4 THEN time '15:00'
                    ELSE        time '16:45'
                END
            ) AT TIME ZONE schedule_timezone
                AS start_time,

            (
                (
                    ws.week_start
                    + sp.weekday_offset
                )::date
                +
                CASE sp.daily_slot
                    WHEN 0 THEN time '09:30'
                    WHEN 1 THEN time '11:15'
                    WHEN 2 THEN time '13:00'
                    WHEN 3 THEN time '14:45'
                    WHEN 4 THEN time '16:30'
                    ELSE        time '18:15'
                END
            ) AT TIME ZONE schedule_timezone
                AS end_time,

            CASE
                WHEN sp.occurrence_number = 1
                    THEN 'LECTURE'

                WHEN sp.course_code LIKE 'DB-%'
                    THEN 'LAB'

                WHEN sp.course_code LIKE 'SPR-%'
                    THEN 'LAB'

                WHEN sp.course_code LIKE 'WEB-%'
                    THEN 'LAB'

                WHEN sp.course_code LIKE 'ML-%'
                    THEN 'LAB'

                WHEN sp.course_code LIKE 'CG-%'
                    THEN 'LAB'

                ELSE 'PRACTICE'
            END AS lesson_type,

            CASE
                WHEN sp.daily_slot IN (0, 3)
                    THEN 'A-201'

                WHEN sp.daily_slot IN (1, 4)
                    THEN 'A-202'

                ELSE 'A-203'
            END AS room,

            format(
                '%s for %s (%s).',
                CASE
                    WHEN sp.occurrence_number = 1
                        THEN 'Weekly lecture'
                    ELSE 'Weekly practical session'
                END,
                sp.course_name,
                sp.group_name
            ) AS description

        FROM scheduled_patterns sp
        CROSS JOIN week_starts ws

        ------------------------------------------------------------------
        -- Six slots/day × five weekdays = 30 available positions.
        --
        -- If V2 is later expanded so far that two weekly lessons for
        -- every group-course relation no longer fit into those 30
        -- positions, do not silently create weekend entries.
        ------------------------------------------------------------------
        WHERE sp.global_slot < 30
    )

    INSERT INTO lessons (
        group_id,
        course_id,
        teacher_id,
        start_time,
        end_time,
        room,
        lesson_type,
        description
    )
    SELECT
        gl.group_id,
        gl.course_id,
        gl.teacher_id,
        gl.start_time,
        gl.end_time,
        gl.room,
        gl.lesson_type,
        gl.description
    FROM generated_lessons gl

    ----------------------------------------------------------------------
    -- V3 normally runs against a database where lessons is still empty.
    --
    -- These checks additionally make the insert safe if lessons were
    -- manually added before the migration was applied.
    ----------------------------------------------------------------------

    WHERE NOT EXISTS (
        SELECT 1
        FROM lessons existing
        WHERE existing.group_id = gl.group_id
          AND existing.start_time < gl.end_time
          AND existing.end_time > gl.start_time
    )

    AND NOT EXISTS (
        SELECT 1
        FROM lessons existing
        WHERE existing.teacher_id = gl.teacher_id
          AND existing.start_time < gl.end_time
          AND existing.end_time > gl.start_time
    )

    AND NOT EXISTS (
        SELECT 1
        FROM lessons existing
        WHERE upper(existing.room) = upper(gl.room)
          AND existing.start_time < gl.end_time
          AND existing.end_time > gl.start_time
    )

    AND NOT EXISTS (
        SELECT 1
        FROM lessons existing
        WHERE existing.group_id = gl.group_id
          AND existing.course_id = gl.course_id
          AND existing.start_time = gl.start_time
          AND existing.end_time = gl.end_time
    );

    GET DIAGNOSTICS inserted_count = ROW_COUNT;

    RAISE NOTICE
        'V3 lesson schedule generated: % lessons from % through %, timezone=%',
        inserted_count,
        first_week,
        last_week + 6,
        schedule_timezone;
END $$ LANGUAGE plpgsql;