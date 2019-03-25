-- Table: shibboleth_data

-- DROP TABLE shibboleth_data;

CREATE TABLE shibboleth_data
(
  host character varying NOT NULL,
  serviceprovider_sibboleth_url character varying,
  sp_side_wayfless boolean,
  target_string character varying,
  entity_id_string character varying,
  shire character varying,
  provider_id character varying,
  CONSTRAINT shibboleth_data_pkey PRIMARY KEY (host)
)
  WITH (
    OIDS=FALSE
  );
-- ALTER TABLE shibboleth_data OWNER TO "services";
