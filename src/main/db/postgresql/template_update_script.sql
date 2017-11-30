DO language plpgsql $$
BEGIN
  RAISE WARNING '#<internal_version_number> INTERNAL VERSION UPGRADE HEADER - START';
END
$$;
INSERT INTO "tInternal" (key, value) VALUES ('Upgrade_to_intVer_<internal_version_number>', NOW());
DO language plpgsql $$
BEGIN
  RAISE WARNING '#<internal_version_number> INTERNAL VERSION UPGRADE HEADER - END';
END
$$;

DO language plpgsql $$
BEGIN
  RAISE WARNING 'start CREATE OR REPLACE function <func_name>';
END
$$;

CREATE OR REPLACE FUNCTION <func_name>

	...

END;

DO language plpgsql $$
BEGIN
  RAISE WARNING 'end CREATE OR REPLACE function <func_name>';
END
$$;

DO language plpgsql $$
BEGIN
  RAISE WARNING '#<internal_version_number> INTERNAL VERSION UPGRADE FOOTER - START';
END
$$;
UPDATE "tInternal" SET value='<internal_version_number>' WHERE key='internalVersion';
DO language plpgsql $$
BEGIN
  RAISE WARNING '#<internal_version_number> INTERNAL VERSION UPGRADE FOOTER - END';
END
$$;
