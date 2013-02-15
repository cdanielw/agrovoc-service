CREATE TABLE agrovocterm (
  termcode     VARCHAR(13)  NOT NULL DEFAULT '0',
  languagecode VARCHAR(2)   NOT NULL DEFAULT '',
  termspell    VARCHAR(170) NOT NULL DEFAULT '',
  statusid     TINYINT(3) UNSIGNED DEFAULT NULL,
  createdate   DATE DEFAULT NULL,
  frequencyiad INT(11) DEFAULT NULL,
  frequencycad INT(11) DEFAULT NULL,
  lastupdate   DATETIME DEFAULT NULL,
  scopeid      VARCHAR(2) DEFAULT NULL,
  idowner      TINYINT(2) DEFAULT '10',
  termsense    TINYINT(2) DEFAULT NULL,
  termoffset   VARCHAR(8) DEFAULT NULL,
  PRIMARY KEY (termcode, languagecode)
);

CREATE TABLE termlink (
  termcode1             VARCHAR(13)      NOT NULL DEFAULT '0',
  termcode2             VARCHAR(13)      NOT NULL DEFAULT '0',
  languagecode1         VARCHAR(2) DEFAULT NULL,
  languagecode2         VARCHAR(2) DEFAULT NULL,
  linktypeid            INT(11) UNSIGNED NOT NULL DEFAULT '0',
  createdate            DATETIME DEFAULT NULL,
  maintenancegroupid    INT(11)          NOT NULL DEFAULT '0',
  newlinktypeid         INT(11) DEFAULT NULL,
  confirm               VARCHAR(1) DEFAULT NULL,
  technique             VARCHAR(5) DEFAULT NULL,
  upddate               DATETIME DEFAULT NULL,
  updmaintenancegroupid INT(11) DEFAULT NULL,
  PRIMARY KEY (termcode1, termcode2, linktypeid)
)

