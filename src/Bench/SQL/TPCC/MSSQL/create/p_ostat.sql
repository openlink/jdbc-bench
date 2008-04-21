create proc ostat
       @w_id         smallint,
       @d_id         tinyint,
       @c_id         int,
       @c_last       char(16) = " "
as
    declare @c_balance     numeric(12,2),
            @c_first       char(16),
            @c_middle      char(2),
            @o_id          int,
            @o_entry_d     datetime,
            @o_carrier_id  smallint,
            @cnt           smallint

    begin tran o

    if (@c_id = 0)
     begin
        /* get customer id and info using last name */
        select @cnt = (count(*)+1)/2
            from tpcc..customer holdlock
            where c_last = @c_last and
                  c_w_id = @w_id and
                  c_d_id = @d_id
        set rowcount @cnt

        select @c_id      = c_id,
               @c_balance = c_balance,
               @c_first   = c_first,
               @c_last    = c_last,
               @c_middle  = c_middle
            from tpcc..customer holdlock
            where c_last = @c_last and
                  c_w_id = @w_id and
                  c_d_id = @d_id
            order by c_w_id, c_d_id, c_last, c_first

        set rowcount 0
     end
    else
     begin
        /* get customer info by id */
        select @c_balance = c_balance,
               @c_first   = c_first,
               @c_middle  = c_middle,
               @c_last    = c_last
            from tpcc..customer holdlock
            where c_id   = @c_id and
                  c_d_id = @d_id and
                  c_w_id = @w_id

        select @cnt = @@rowcount
     end

    /* if no such customer */
    if (@cnt = 0)
     begin
        raiserror('Customer not found',18,1)
        goto custnotfound
     end

    /* get order info */
    select @o_id         = o_id,
           @o_entry_d    = o_entry_d,
           @o_carrier_id = o_carrier_id
        from tpcc..orders holdlock
        where o_w_id = @w_id and
              o_d_id = @d_id and
              o_c_id = @c_id

    /* select order lines for the current order */
    select ol_supply_w_id,
           ol_i_id,
           ol_quantity,
           ol_amount,
           ol_delivery_d
        from tpcc..order_line holdlock
        where ol_o_id = @o_id and
              ol_d_id = @d_id and
              ol_w_id = @w_id

custnotfound:

    commit tran o

    /* return data to client */

    select @c_id,
           @c_last,
           @c_first,
           @c_middle,
           @o_entry_d,
           @o_carrier_id,
           @c_balance,
           @o_id
