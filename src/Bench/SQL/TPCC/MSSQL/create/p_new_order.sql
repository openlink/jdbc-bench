create proc new_order

       @w_id         smallint,

       @d_id         tinyint,

       @c_id         int,

       @o_ol_cnt     tinyint,

       @o_all_local  tinyint,

       @i_id1  int = 0, @s_w_id1  smallint = 0, @ol_qty1  smallint = 0,

       @i_id2  int = 0, @s_w_id2  smallint = 0, @ol_qty2  smallint = 0,

       @i_id3  int = 0, @s_w_id3  smallint = 0, @ol_qty3  smallint = 0,

       @i_id4  int = 0, @s_w_id4  smallint = 0, @ol_qty4  smallint = 0,

       @i_id5  int = 0, @s_w_id5  smallint = 0, @ol_qty5  smallint = 0,

       @i_id6  int = 0, @s_w_id6  smallint = 0, @ol_qty6  smallint = 0,

       @i_id7  int = 0, @s_w_id7  smallint = 0, @ol_qty7  smallint = 0,

       @i_id8  int = 0, @s_w_id8  smallint = 0, @ol_qty8  smallint = 0,

       @i_id9  int = 0, @s_w_id9  smallint = 0, @ol_qty9  smallint = 0,

       @i_id10 int = 0, @s_w_id10 smallint = 0, @ol_qty10 smallint = 0,

       @i_id11 int = 0, @s_w_id11 smallint = 0, @ol_qty11 smallint = 0,

       @i_id12 int = 0, @s_w_id12 smallint = 0, @ol_qty12 smallint = 0,

       @i_id13 int = 0, @s_w_id13 smallint = 0, @ol_qty13 smallint = 0,

       @i_id14 int = 0, @s_w_id14 smallint = 0, @ol_qty14 smallint = 0,

       @i_id15 int = 0, @s_w_id15 smallint = 0, @ol_qty15 smallint = 0

as

    declare @w_tax       numeric(4,4),

            @d_tax       numeric(4,4),

            @c_last      char(16),

            @c_credit    char(2),

            @c_discount  numeric(4,4),

            @i_price     numeric(5,2),

            @i_name      char(24),

            @i_data      char(50),

            @o_entry_d   datetime,

            @remote_flag int,

            @s_quantity  smallint,

            @s_data      char(50),

            @s_dist      char(24),

            @li_no       int,

            @o_id        int,

            @commit_flag int,

            @li_id       int,

            @li_s_w_id   smallint,

            @li_qty      smallint,

            @ol_number   int,

            @c_id_local  int

 

  begin



      begin transaction n



      /* get district tax and next available order id and update */

      /* plus initialize local variables */



      update tpcc..district

             set @d_tax       = d_tax,

                 @o_id        = d_next_o_id,

                 d_next_o_id  = d_next_o_id + 1,

                 @o_entry_d   = getdate(),

                 @li_no=0,

                 @commit_flag = 1

             where d_w_id = @w_id and d_id = @d_id



      /* process orderlines */

      while(@li_no < @o_ol_cnt)

       begin



         /* Set i_id, s_w_id and qty for this lineitem */



         select @li_no=@li_no+1,

                @li_id = case @li_no

                           when 0  then @i_id1

                           when 1  then @i_id2

                           when 2  then @i_id3

                           when 3  then @i_id4

                           when 4  then @i_id5

                           when 5  then @i_id6

                           when 6  then @i_id7

                           when 7  then @i_id8

                           when 8  then @i_id9

                           when 9  then @i_id10

                           when 10 then @i_id11

                           when 11 then @i_id12

                           when 12 then @i_id13

                           when 13 then @i_id14

                           when 14 then @i_id15

                         end,

                @li_s_w_id = case @li_no

                           when 0  then @s_w_id1

                           when 1  then @s_w_id2

                           when 2  then @s_w_id3

                           when 3  then @s_w_id4

                           when 4  then @s_w_id5

                           when 5  then @s_w_id6

                           when 6  then @s_w_id7

                           when 7  then @s_w_id8

                           when 8  then @s_w_id9

                           when 9  then @s_w_id10

                           when 10 then @s_w_id11

                           when 11 then @s_w_id12

                           when 12 then @s_w_id13

                           when 13 then @s_w_id14

                           when 14 then @s_w_id15

                         end,

                @li_qty = case @li_no

                           when 0  then @ol_qty1

                           when 1  then @ol_qty2

                           when 2  then @ol_qty3

                           when 3  then @ol_qty4

                           when 4  then @ol_qty5

                           when 5  then @ol_qty6

                           when 6  then @ol_qty7

                           when 7  then @ol_qty8

                           when 8  then @ol_qty9

                           when 9  then @ol_qty10

                           when 10 then @ol_qty11

                           when 11 then @ol_qty12

                           when 12 then @ol_qty13

                           when 13 then @ol_qty14

                           when 14 then @ol_qty15

                         end



         /* get item data (no one updates item) */



         select @i_price = i_price,

                @i_name  = i_name,

                @i_data  = i_data

                from tpcc..item (tablock holdlock)

                where i_id = @li_id



         /* if there actually is an item with this id, go to work */



         if (@@rowcount > 0)

          begin

           update tpcc..stock set s_ytd       = s_ytd + @li_qty,

                            s_quantity  = s_quantity - @li_qty +

                                     case when (s_quantity - @li_qty < 10)

                                          then 91 else 0 end,

                            @s_quantity = s_quantity,

                            s_cnt_order = s_cnt_order + 1,

                            s_cnt_remote = s_cnt_remote +

                        case when (@li_s_w_id = @w_id) then 0 else 1 end,

                            @s_data     = s_data,

                            @s_dist     = case @d_id

                                            when 1  then s_dist_01

                                            when 2  then s_dist_02

                                            when 3  then s_dist_03

                                            when 4  then s_dist_04

                                            when 5  then s_dist_05

                                            when 6  then s_dist_06

                                            when 7  then s_dist_07

                                            when 8  then s_dist_08

                                            when 9  then s_dist_09

                                            when 10 then s_dist_10

                                          end

                  where s_i_id = @li_id and s_w_id = @li_s_w_id



           /* insert order_line data (using data from item and stock) */



           insert into tpcc..order_line

                  values(@o_id,          /* from district update */

                         @d_id,          /* input param          */

                         @w_id,          /* input param          */

                         @li_no,         /* orderline number     */

                         @li_id,         /* lineitem id          */

                         @li_s_w_id,     /* lineitem warehouse   */

                         'dec 31, 1889', /* constant             */

                         @li_qty,        /* lineitem qty         */

                         @i_price * @li_qty, /* ol_amount        */

                         @s_dist)        /* from stock           */





           /* send line-item data to client */



           select @i_name, @s_quantity,

                  b_g = case when ((patindex('%ORIGINAL%',@i_data) > 0) and

                                   (patindex('%ORIGINAL%',@s_data) > 0))

                        then 'B' else 'G' end,

                  @i_price,

                  @i_price * @li_qty



          end  -- /* condition: if (@@rowcount > 0) */

         else

          begin

           /* no item found - triggers rollback condition */

           select '', '',0

           select @commit_flag = 0

         end



       end -- /* of orderlines loop while(@li_no < @o_ol_cnt) */



      /* get customer last name, discount, and credit rating */



      select @c_last     = c_last,

             @c_discount = c_discount,

             @c_credit   = c_credit,

             @c_id_local = c_id

          from tpcc..customer holdlock

          where c_id    = @c_id and

                c_w_id  = @w_id and

                c_d_id  = @d_id





      /* insert fresh row into orders table */



      insert into tpcc..orders values(@o_id, @d_id, @w_id, @c_id_local,

                                @o_entry_d, 0, @o_ol_cnt, @o_all_local)





      /* insert corresponding row into new-order table */



      insert into tpcc..new_order values (@o_id, @d_id, @w_id)



      /* select warehouse tax */



      select @w_tax = w_tax

             from tpcc..warehouse holdlock

             where w_id = @w_id



      if (@commit_flag = 1)

          commit transaction n

      else

          /* all that work for nuthin!!! */

          rollback transaction n





      /* return order data to client */

      select @w_tax,

             @d_tax,

             @o_id,

             @c_last,

             @c_discount,

             @c_credit,

             @o_entry_d,

             @commit_flag



  end


