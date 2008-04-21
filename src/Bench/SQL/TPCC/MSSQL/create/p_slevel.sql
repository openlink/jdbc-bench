create proc slevel
       @w_id         smallint,
       @d_id         tinyint,
       @threshhold   smallint
as
    declare @o_id int

    select @o_id = d_next_o_id
        from tpcc..district
        where d_w_id = @w_id and d_id = @d_id

    select count(*) from tpcc..stock,
           (select distinct(ol_i_id) from tpcc..order_line
               where ol_w_id   = @w_id and
                     ol_d_id   = @d_id and
                     ol_o_id between (@o_id-20) and (@o_id-1)) OL

           where s_w_id     = @w_id and
                 s_i_id     = OL.ol_i_id and
                 s_quantity < @threshhold
