-- ***********************************************
-- Script for upgrade from version 4.0.6 to 4.1.0
-- ***********************************************

\set ON_ERROR_STOP on

DO language plpgsql $$
BEGIN
  RAISE WARNING '#16 INTERNAL VERSION UPGRADE HEADER - START';
END
$$;
INSERT INTO "tInternal" (key, value) VALUES ('Upgrade_to_intVer_16', NOW());
DO language plpgsql $$
BEGIN
  RAISE WARNING '#16 INTERNAL VERSION UPGRADE HEADER - END';
END
$$;

DO language plpgsql $$
BEGIN
  RAISE WARNING 'start CREATE function sp_insert_checkpoints';
END
$$;

CREATE FUNCTION sp_insert_checkpoints(_query TEXT)
RETURNS VOID as $func$
BEGIN
	EXECUTE _query;
END;
$func$ LANGUAGE plpgsql;

DO language plpgsql $$
BEGIN
  RAISE WARNING 'end CREATE function sp_insert_checkpoints';
END
$$;