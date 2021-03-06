CREATE PROCEDURE ODBC_BENCHMARK
        @histid  int, 
        @acct    int, 
        @teller  int, 
        @branch  int, 
        @delta   float,
        @balance float output,
        @filler  char(22)
AS
BEGIN TRANSACTION
UPDATE    account
    SET   balance = balance + @delta
    WHERE account = @acct
SELECT    @balance = balance
    FROM  account 
    WHERE account = @acct
UPDATE    teller
    SET   balance = balance + @delta
    WHERE teller  = @teller
UPDATE    branch
    SET   balance = balance + @delta
    WHERE branch  = @branch
INSERT history
	(histid, account, teller, branch, amount, timeoftxn, filler)
VALUES
	(@histid, @acct, @teller, @branch, @delta, getdate(), @filler)
COMMIT TRANSACTION
