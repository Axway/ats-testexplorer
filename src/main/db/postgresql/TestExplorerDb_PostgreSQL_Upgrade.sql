-- ***********************************************
-- Script for upgrade from version 4.0.10 to 4.0.11.
-- ***********************************************

\set ON_ERROR_STOP on

DO language plpgsql $$
BEGIN
  RAISE WARNING '#19 INTERNAL VERSION UPGRADE HEADER - START';
END
$$;


DO language plpgsql $$
BEGIN
  RAISE WARNING '#19 INTERNAL VERSION UPGRADE FOOTER - START';
END
$$;
-- nothing changed in DB - keep old internal version
-- UPDATE "tInternal" SET value='19' WHERE key='internalVersion';
INSERT INTO "tInternal" (key, value) VALUES ('Upgrade_to_intVer_19', NOW());

DO language plpgsql $$
BEGIN
  RAISE WARNING '#19 INTERNAL VERSION UPGRADE HEADER - END';
END
$$;
