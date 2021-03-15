-- ***********************************************
-- Script for upgrade from version 4.0.6 to 4.0.7.
-- ***********************************************

\set ON_ERROR_STOP on

DO language plpgsql $$
BEGIN
  RAISE WARNING '#19 INTERNAL VERSION UPGRADE HEADER - START';
END
$$;
INSERT INTO "tInternal" (key, value) VALUES ('Upgrade_to_intVer_19', NOW());
DO language plpgsql $$
BEGIN
  RAISE WARNING '#19 INTERNAL VERSION UPGRADE HEADER - END';
END
$$;

-- nothing changed

DO language plpgsql $$
BEGIN
  RAISE WARNING '#19 INTERNAL VERSION UPGRADE FOOTER - START';
END
$$;

UPDATE "tInternal" SET value='19' WHERE key='internalVersion';
DO language plpgsql $$
BEGIN
  RAISE WARNING '#19 INTERNAL VERSION UPGRADE FOOTER - END';
END
$$;
