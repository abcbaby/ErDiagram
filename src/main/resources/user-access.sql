CREATE USER dragon
  IDENTIFIED BY dragon
  DEFAULT TABLESPACE users
  TEMPORARY TABLESPACE temp
  QUOTA 20M on users
  PASSWORD EXPIRE;
  
CREATE TABLE dragon.users ( 
    user_id number(10) NOT NULL,
    username varchar2(50) NOT NULL,
    firstname varchar2(50) NOT NULL,
    lastname varchar2(50) NOT NULL,
    middlename char(1),
    email varchar2(150),
    CONSTRAINT users_pk PRIMARY KEY (user_id)
);

CREATE TABLE dragon.roles ( 
    role_id number(10) NOT NULL,
    role varchar2(50) NOT NULL,
    description varchar2(150),
    CONSTRAINT roles_pk PRIMARY KEY (role_id)
);

CREATE TABLE dragon.user_roles ( 
    user_id number(10) NOT NULL,
    role_id number(10) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles1 FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_user_roles2 FOREIGN KEY (role_id) REFERENCES roles(role_id)
);
