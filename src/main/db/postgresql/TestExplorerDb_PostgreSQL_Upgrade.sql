-- ***********************************************
-- Script for upgrade from version 4.0.6 to 4.0.7.
-- ***********************************************

\set ON_ERROR_STOP on

DO language plpgsql $$
BEGIN
  RAISE WARNING '#18 INTERNAL VERSION UPGRADE HEADER - START';
END
$$;
INSERT INTO "tInternal" (key, value) VALUES ('Upgrade_to_intVer_18', NOW());
DO language plpgsql $$
BEGIN
  RAISE WARNING '#18 INTERNAL VERSION UPGRADE HEADER - END';
END
$$;

-- Add changes for 4.0.7 (next release) here
--
--

DO language plpgsql $$
BEGIN
  RAISE WARNING '#18 INTERNAL VERSION UPGRADE FOOTER - START';
END
$$;
UPDATE "tInternal" SET value='18' WHERE key='internalVersion';
DO language plpgsql $$
BEGIN
  RAISE WARNING '#18 INTERNAL VERSION UPGRADE FOOTER - END';
END
$$;
