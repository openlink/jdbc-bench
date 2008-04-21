create proc delivery  @w_id smallint, @o_carrier_id smallint
as
    declare @d_id tinyint,
            @o_id int,
            @c_id int,
            @total numeric(12,2),
            @oid1 int,
            @oid2 int,
            @oid3 int,
            @oid4 int,
            @oid5 int,
            @oid6 int,
            @oid7 int,
            @oid8 int,
            @oid9 int,
            @oid10 int

    select @d_id = 0

    begin tran d
 
        while (@d_id < 10)
        begin

            select @d_id = @d_id + 1,
                   @total = 0,
                   @o_id = 0

            select @o_id = min(no_o_id)
                   from tpcc..new_order holdlock
                   where no_w_id = @w_id and no_d_id = @d_id

            if(@@rowcount <> 0)
            begin

--              /* claim the order for this district */

                delete tpcc..new_order
                       where no_w_id = @w_id and no_d_id = @d_id
                         and no_o_id = @o_id

--              /* set carrier_id on this order (and get customer id) */

                update tpcc..orders
                       set o_carrier_id = @o_carrier_id, @c_id = o_c_id
                       where o_w_id = @w_id and o_d_id = @d_id
                         and o_id = @o_id

--      /* set date in all lineitems for this order (and sum amounts) */


                update tpcc..order_line
                       set ol_delivery_d = getdate(),
                           @total = @total + ol_amount
                       where ol_w_id = @w_id and
                             ol_d_id = @d_id and
                             ol_o_id = @o_id

--      /* accumulate lineitem amounts for this order into customer */

                update tpcc..customer
                       set c_balance      = c_balance + @total,
                           c_cnt_delivery = c_cnt_delivery + 1
                       where c_w_id = @w_id and
                             c_d_id = @d_id and
                             c_id = @c_id

            end

            select @oid1  = case @d_id when 1  then @o_id else @oid1  end,
                   @oid2  = case @d_id when 2  then @o_id else @oid2  end,
                   @oid3  = case @d_id when 3  then @o_id else @oid3  end,
                   @oid4  = case @d_id when 4  then @o_id else @oid4  end,
                   @oid5  = case @d_id when 5  then @o_id else @oid5  end,
                   @oid6  = case @d_id when 6  then @o_id else @oid6  end,
                   @oid7  = case @d_id when 7  then @o_id else @oid7  end,
                   @oid8  = case @d_id when 8  then @o_id else @oid8  end,
                   @oid9  = case @d_id when 9  then @o_id else @oid9  end,
                   @oid10 = case @d_id when 10 then @o_id else @oid10 end


        end

    commit tran d

    select @oid1,
           @oid2,
           @oid3,
           @oid4,
           @oid5,
           @oid6,
           @oid7,
           @oid8,
           @oid9,
           @oid10


