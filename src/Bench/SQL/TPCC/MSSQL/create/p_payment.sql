create procedure payment
       @w_id         smallint,
       @c_w_id       smallint,
       @h_amount     float,
       @d_id         tinyint,
       @c_d_id       tinyint,
       @c_id         int,
       @c_last       char(16) = " "
as
    declare @w_street_1    char(20),
            @w_street_2    char(20),
            @w_city        char(20),
            @w_state       char(2),
            @w_zip         char(9),
            @w_name        char(10),
            @d_street_1    char(20),
            @d_street_2    char(20),
            @d_city        char(20),
            @d_state       char(2),
            @d_zip         char(9),
            @d_name        char(10),
            @c_first       char(16),
            @c_middle      char(2),
            @c_street_1    char(20),
            @c_street_2    char(20),
            @c_city        char(20),
            @c_state       char(2),
            @c_zip         char(9),
            @c_phone       char(16),
            @c_since       datetime,
            @c_credit      char(2),
            @c_credit_lim  float,
            @c_balance     float,
            @c_ytd_payment float,
            @c_discount    float,
            @data1         char(250),
            @data2         char(250),
            @c_data_1      char(250),
            @c_data_2      char(250),
            @datetime      datetime,
            @w_ytd         float,
            @d_ytd         float,
            @cnt           smallint,
            @val           smallint,
            @screen_data   char(200),
            @d_id_local    tinyint,
            @w_id_local    smallint,
            @c_id_local    int


begin

    select @screen_data = ''

    begin transaction p

    /* get payment date */

    select @datetime = getdate()

    if (@c_id = 0)
     begin
        /* get customer id and info using last name */
        select @cnt = count(*)
            from tpcc..customer holdlock
            where c_last = @c_last and
                  c_w_id = @c_w_id and
                  c_d_id = @c_d_id

        select @val = (@cnt + 1) / 2
        set rowcount @val

        select @c_id      = c_id
            from tpcc..customer holdlock
            where c_last = @c_last and
                  c_w_id = @c_w_id and
                  c_d_id = @c_d_id
            order by c_w_id, c_d_id, c_last, c_first

        set rowcount 0
     end

    /* get customer info and update balances */

    update tpcc..customer set
        @c_balance = c_balance - @h_amount,
        c_balance = @c_balance,
        c_cnt_payment   = c_cnt_payment + 1,
        @c_ytd_payment   = c_ytd_payment + 10.0 /* @h_amount */,
        @c_first         = c_first,
        @c_middle        = c_middle,
        @c_last          = c_last,
        @c_street_1      = c_street_1,
        @c_street_2      = c_street_2,
        @c_city          = c_city,
        @c_state         = c_state,
        @c_zip           = c_zip,
        @c_phone         = c_phone,
        @c_credit        = c_credit,
        @c_credit_lim    = c_credit_lim,
        @c_discount      = c_discount,
        @c_since         = c_since,
        @data1           = c_data_1,
        @data2           = c_data_2,
        @c_id_local      = c_id
       where c_id   = @c_id and
             c_w_id = @c_w_id and
             c_d_id = @c_d_id

    /* if customer has bad credit get some more info */

    if (@c_credit = 'BC')
     begin
        /* compute new info (Kublissa on myos substring) */
        select @c_data_2 = substring(@data1,209,42) +
                           substring(@data2,1,208)
        select @c_data_1 = convert(char(5),@c_id) + 
                           convert(char(4),@c_d_id) +
                           convert(char(5),@c_w_id) +
                           convert(char(4),@d_id) +
                           convert(char(5),@w_id) +
                           convert(char(19),@h_amount) +
                           substring(@data1, 1, 208)

        /* update customer info */
        update tpcc..customer set
               c_data_1 = @c_data_1,
               c_data_2 = @c_data_2
            where c_id   = @c_id and
                  c_w_id = @c_w_id and
                  c_d_id = @c_d_id


        select @screen_data = substring(@c_data_1,1,200)

     end


    /* get district data and update year-to-date */

    update tpcc..district
        set d_ytd       = d_ytd + @h_amount,
            @d_street_1 = d_street_1,
            @d_street_2 = d_street_2,
            @d_city     = d_city,
            @d_state    = d_state,
            @d_zip      = d_zip,
            @d_name     = d_name,
            @d_id_local = d_id
        where d_w_id = @w_id and
              d_id   = @d_id

    /* get warehouse data and update year-to-date */

    update tpcc..warehouse
        set w_ytd       = w_ytd + @h_amount,
            @w_street_1 = w_street_1,
            @w_street_2 = w_street_2,
            @w_city     = w_city,
            @w_state    = w_state,
            @w_zip      = w_zip,
            @w_name     = w_name,
            @w_id_local = w_id
        where w_id = @w_id

    /* create history record */

    insert into tpcc..history
        values(@c_id_local,
               @c_d_id,
               @c_w_id,
               @d_id_local,
               @w_id_local,
               @datetime,
               @h_amount,
               @w_name + '    ' + @d_name) /* 10 + 4 + 10 = 24 */

    commit tran p

    /* return data to client */

    select @c_id,
           @c_last,
           @datetime,
           @w_street_1,
           @w_street_2,
           @w_city,
           @w_state,
           @w_zip,
           @d_street_1,
           @d_street_2,
           @d_city,
           @d_state,
           @d_zip,
           @c_first,
           @c_middle,
           @c_street_1,
           @c_street_2,
           @c_city,
           @c_state,
           @c_zip,
           @c_phone,
           @c_since,
           @c_credit,
           @c_credit_lim,
           @c_discount,
           @c_balance,
           @screen_data
end
