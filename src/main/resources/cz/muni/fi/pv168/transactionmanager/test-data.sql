/**
 * Author:  Miroslav Kubus
 * Created: 19.4.2016
 */

INSERT INTO account (number, holder, balance) VALUES ('123','Miroslav Kubus',1000);
INSERT INTO account (number, holder, balance) VALUES ('123123','Miroslav Kubus 2',10000);
INSERT INTO account (number, holder, balance) VALUES ('123456','Miroslav Kubus 3',5000);

INSERT INTO payment (fromAccount, toAccount, amount, date) VALUES (1,2,1000,'2016-10-25');
INSERT INTO payment (fromAccount, toAccount, amount, date) VALUES (2,3,5000,'2016-12-24');