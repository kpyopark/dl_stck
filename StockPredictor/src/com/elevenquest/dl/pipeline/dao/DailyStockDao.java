package com.elevenquest.dl.pipeline.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elevenquest.dl.pipeline.post.PredictMetric;

public class DailyStockDao extends BaseDao {
	private static Logger log = LoggerFactory.getLogger(DailyStockDao.class);

	public static String getDailyStockLearningDataQuery(String stockId, String startDate) {
		return "with tb_tx_date as ( \n" + 
				"    select distinct standard_date \n" + 
				"      from tb_company_stock_daily \n" + 
				"     where stock_id = 'A005930' and standard_date >= '"+ startDate + "' \n" + 
				"     order by 1 \n" + 
				"     limit 1200 \n" + 
				") \n" + 
				", tb_tx_last_date as ( \n" + 
				"    select max(standard_date) as last_tx_date, \n" + 
				"           to_date(max(standard_date),'YYYYMMDD') as last_tx_datetype \n" + 
				"      from tb_tx_date \n" + 
				") \n" + 
				", tb_predict_date as ( \n" + 
				"    select last_tx_datetype + case when extract(dow from last_tx_datetype) between 1 and 4 then '1 day'::interval else '3 day'::interval end as predict_target_date \n" + 
				"      from tb_tx_last_date \n" + 
				") \n" + 
				", tb_tx_date_with_predict as ( \n" + 
				"    select * from tb_tx_date \n" + 
				"    union all \n" + 
				"    select to_char(predict_target_date, 'YYYYMMDD') from tb_predict_date \n" + 
				") \n" + 
				", tb_settings as ( \n" + 
				"    select 0 as day_bias, \n" + 
				"           '" + stockId + "'::text as stock_id, \n" + 
				"           standard_date as target_date, \n" + 
				"           to_date(standard_date,'YYYYMMDD') as target_datetype, \n" + 
				"           to_char(to_date(standard_date, 'YYYYMMDD') - '1 year'::interval, 'YYYYMMDD') as start_date \n" + 
				"      from tb_tx_date_with_predict \n" + 
				") \n" + 
				", tb_std_date as ( \n" + 
				"    select b.start_date, \n" + 
				"           b.target_date, \n" + 
				"           extract(doy from target_datetype) as target_doy, \n" + 
				"           extract(dow from target_datetype) as target_dow, \n" + 
				"           extract(year from target_datetype) as target_year, \n" + 
				"           b.stock_id, \n" + 
				"           day_bias \n" + 
				"      from tb_settings b \n" + 
				") \n" + 
				", tb_daily_stock_predict as ( \n" + 
				"    select stock_id,  \n" + 
				"           to_char(predict_target_date, 'YYYYMMDD') as standard_date, \n" + 
				"	   null::bigint, -- stock_price \n" + 
				"	   null::bigint, \n" + 
				"	   null::double precision, -- net_change_ratio \n" + 
				"	   null::bigint, \n" + 
				"	   null::bigint, -- bid_price \n" + 
				"	   null::bigint, \n" + 
				"	   null::bigint, -- today_low \n" + 
				"	   null::bigint, \n" + 
				"	   null::bigint, -- volume_amount \n" + 
				"	   null::bigint,  \n" + 
				"	   null::double precision, -- par_value \n" + 
				"	   null::varchar, \n" + 
				"	   null::bigint, -- ordinary_share \n" + 
				"	   null::bigint, \n" + 
				"	   null::varchar, -- company_name \n" + 
				"	   null::varchar, \n" + 
				"	   null::varchar  -- security_type \n" + 
				"      from tb_settings a join tb_predict_date b on (a.target_date = to_char(predict_target_date, 'YYYYMMDD')) \n" + 
				") \n" + 
				", tb_daily_stock_ext_ as ( \n" + 
				"    select a.*,  \n" + 
				"           to_date(standard_date,'YYYYMMDD') as std_date, \n" + 
				"           to_date(standard_date,'YYYYMMDD') + ('' || day_bias || 'day')::interval as bias_date,  \n" + 
				"           target_date, \n" + 
				"           target_dow, \n" + 
				"           target_doy, \n" + 
				"           target_year \n" + 
				"           -- stock_price lag(stock_price) over (partition by a.stock_id, b.target_date order by a.standard_date) as prev_close_price \n" + 
				"      from tb_company_stock_daily a join tb_std_date b using(stock_id) \n" + 
				"     where a.standard_date >= b.start_date  \n" + 
				"       and a.standard_date <= b.target_date \n" + 
				"     union all \n" + 
				"    select a.*,  \n" + 
				"           to_date(standard_date,'YYYYMMDD') as std_date, \n" + 
				"           to_date(standard_date,'YYYYMMDD') + ('' || day_bias || 'day')::interval as bias_date,  \n" + 
				"           target_date, \n" + 
				"           target_dow, \n" + 
				"           target_doy, \n" + 
				"           target_year \n" + 
				"           -- stock_price lag(stock_price) over (partition by a.stock_id, b.target_date order by a.standard_date) as prev_close_price \n" + 
				"      from tb_daily_stock_predict a join tb_std_date b using(stock_id) \n" + 
				"     where a.standard_date >= b.start_date  \n" + 
				"       and a.standard_date <= b.target_date \n" + 
				") \n" + 
				", tb_daily_stock_ext as ( \n" + 
				"    select a.*, \n" + 
				"           lag(stock_price) over (partition by stock_id, target_date order by standard_date) as prev_close_price \n" + 
				"      from tb_daily_stock_ext_ a \n" + 
				") \n" + 
				", tb_daily_stock_with_year_week as ( \n" + 
				"    select a.*, extract(year from bias_date) as start_year,  \n" + 
				"           extract(week from bias_date) + case when extract(isodow from bias_date) > 5 then 1 else 0 end as bias_week, \n" + 
				"           extract(isodow from std_date) as dow \n" + 
				"      from tb_daily_stock_ext a \n" + 
				") \n" + 
				", tb_week_from_to as ( \n" + 
				"    select stock_id,  \n" + 
				"           min(standard_date) as week_open_date,  \n" + 
				"           max(standard_date) as week_close_date \n" + 
				"      from tb_daily_stock_with_year_week \n" + 
				"     group by stock_id, start_year, bias_week \n" + 
				"     order by 1,2,3 \n" + 
				") \n" + 
				", tb_week_info as ( \n" + 
				"    select stock_id,  \n" + 
				"           week_open_date,  \n" + 
				"           week_close_date,  \n" + 
				"           lag(week_close_date) over (order by week_open_date) as prev_week_close_date \n" + 
				"      from tb_week_from_to \n" + 
				") \n" + 
				", tb_daily_stock_with_week_info as ( \n" + 
				"    select a.*, \n" + 
				"           min(standard_date) over (partition by stock_id, start_year, bias_week) as week_open_date, \n" + 
				"           max(standard_date) over (partition by stock_id, start_year, bias_week) as week_close_date, \n" + 
				"           min(today_low) over (partition by stock_id, start_year, bias_week) as week_low_price, \n" + 
				"           max(today_high) over (partition by stock_id, start_year, bias_week) as week_high_price \n" + 
				"      from tb_daily_stock_with_year_week a \n" + 
				") \n" + 
				", tb_daily_stock_with_week_open_close as ( \n" + 
				"    select a.*, \n" + 
				"           b.open_price as week_open_price, \n" + 
				"           c.stock_price as week_close_price, \n" + 
				"           e.stock_price as prev_week_close_price \n" + 
				"      from tb_daily_stock_with_week_info a  \n" + 
				"           left outer join tb_daily_stock_ext b on (a.stock_id = b.stock_id and a.target_date = b.target_date and a.week_open_date = b.standard_date) \n" + 
				"           left outer join tb_daily_stock_ext c on (a.stock_id = c.stock_id and a.target_date = c.target_date and a.week_close_date = c.standard_date) \n" + 
				"           left outer join tb_week_info d on (a.stock_id = d.stock_id and a.week_open_date = d.week_open_date) \n" + 
				"           left outer join tb_daily_stock_ext e on (d.stock_id = e.stock_id and a.target_date = e.target_date and d.prev_week_close_date = e.standard_date) \n" + 
				") \n" + 
				", tb_weekly_stock as ( \n" + 
				"    select distinct  \n" + 
				"           stock_id,  \n" + 
				"           target_date, \n" + 
				"           week_open_date, \n" + 
				"           week_open_price, \n" + 
				"           week_low_price,  \n" + 
				"           week_high_price, \n" + 
				"           week_close_price, \n" + 
				"           prev_week_close_price \n" + 
				"      from tb_daily_stock_with_week_open_close \n" + 
				") \n" + 
				", tb_weekly_stock_ratio as ( \n" + 
				"    select stock_id,  \n" + 
				"           target_date, \n" + 
				"           week_open_date, \n" + 
				"           log(week_open_price::float / prev_week_close_price) as week_open_ratio, \n" + 
				"           log(week_low_price::float / prev_week_close_price) as week_low_ratio, \n" + 
				"           log(week_high_price::float / prev_week_close_price) as week_high_ratio, \n" + 
				"           log(week_close_price::float / prev_week_close_price) as week_close_ratio, \n" + 
				"           prev_week_close_price, \n" + 
				"           row_number() over (partition by stock_id, target_date order by week_open_date desc) as week_rank \n" + 
				"      from tb_weekly_stock \n" + 
				") \n" + 
				", tb_weekly_stock_ratio_with_limit as ( \n" + 
				"    select * \n" + 
				"      from tb_weekly_stock_ratio \n" + 
				"     where week_rank between 13 and 48 \n" + 
				") \n" + 
				", tb_daily_and_weekly_stock as ( \n" + 
				"    select * \n" + 
				"      from tb_daily_stock_with_week_open_close a join tb_weekly_stock_ratio_with_limit b using (stock_id, target_date, week_open_date) \n" + 
				") \n" + 
				", tb_daily_stock_ratio as ( \n" + 
				"    select stock_id,  \n" + 
				"           standard_date, \n" + 
				"           target_date, \n" + 
				"           target_year, \n" + 
				"           target_doy, \n" + 
				"           target_dow, \n" + 
				"           log(open_price::float / prev_close_price) as day_open_ratio, \n" + 
				"           log(today_low::float / prev_close_price) as day_low_ratio, \n" + 
				"           log(today_high::float / prev_close_price) as day_high_ratio, \n" + 
				"           log(stock_price::float / prev_close_price) as day_close_ratio, \n" + 
				"           row_number() over (partition by stock_id, target_date order by standard_date desc) as day_rank \n" + 
				"      from tb_daily_stock_with_week_open_close \n" + 
				") \n" + 
				", tb_daily_stock_ratio_with_limit as ( \n" + 
				"    select * \n" + 
				"      from tb_daily_stock_ratio \n" + 
				"     where day_rank between 2 and 61 \n" + 
				") \n" + 
				", tb_daily_stock_ratio_target as ( \n" + 
				"    select * \n" + 
				"      from tb_daily_stock_ratio \n" + 
				"     where day_rank = 1 \n" + 
				") \n" + 
				"-- select stock_id, target_date, standard_date, day_open_ratio, day_low_ratio, day_high_ratio, day_close_ratio from tb_daily_stock_ratio_with_limit order by stock_id, target_date, standard_date \n" + 
				", tb_result as ( \n" + 
				"select stock_id, \n" + 
				"       target_date,  \n" + 
				"       target_year,  \n" + 
				"       target_doy, \n" + 
				"       target_dow, \n" + 
				"       array(select day_open_ratio from tb_daily_stock_ratio_with_limit a where a.stock_id = b.stock_id and a.target_date = b.target_date order by stock_id, target_date, standard_date) as day_open_ratio, \n" + 
				"       array(select day_low_ratio from tb_daily_stock_ratio_with_limit a where a.stock_id = b.stock_id and a.target_date = b.target_date order by stock_id, target_date, standard_date) as day_low_ratio, \n" + 
				"       array(select day_high_ratio from tb_daily_stock_ratio_with_limit a where a.stock_id = b.stock_id and a.target_date = b.target_date order by stock_id, target_date, standard_date) as day_high_ratio, \n" + 
				"       array(select day_close_ratio from tb_daily_stock_ratio_with_limit a where a.stock_id = b.stock_id and a.target_date = b.target_date order by stock_id, target_date, standard_date) as day_close_ratio, \n" + 
				"       array(select week_open_ratio from tb_weekly_stock_ratio_with_limit a where a.stock_id = b.stock_id and a.target_date = b.target_date order by stock_id, target_date, week_open_date) as week_open_ratio, \n" + 
				"       array(select week_low_ratio from tb_weekly_stock_ratio_with_limit a where a.stock_id = b.stock_id and a.target_date = b.target_date order by stock_id, target_date, week_open_date) as week_low_ratio, \n" + 
				"       array(select week_high_ratio from tb_weekly_stock_ratio_with_limit a where a.stock_id = b.stock_id and a.target_date = b.target_date order by stock_id, target_date, week_open_date) as week_high_ratio, \n" + 
				"       array(select week_close_ratio from tb_weekly_stock_ratio_with_limit a where a.stock_id = b.stock_id and a.target_date = b.target_date order by stock_id, target_date, week_open_date) as week_close_ratio, \n" + 
				"       day_open_ratio as target_open_ratio, \n" + 
				"       day_low_ratio as target_low_ratio, \n" + 
				"       day_high_ratio as target_high_ratio, \n" + 
				"       day_close_ratio as target_close_ratio \n" + 
				"  from tb_daily_stock_ratio_target b \n" + 
				") \n" + 
				"select -- stock_id, \n" + 
				"       -- target_date, \n" + 
				"       -- target_date::numeric / 99999999 as target_date,  \n" + 
				"       target_year / 9999 as target_year_ratio,  \n" + 
				"       target_doy / 366 as target_doy_ratio, \n" + 
				"       target_dow / 7 as target_dow_ratio \n" + 
				"       ,day_open_ratio[1] \n" + 
				"	,day_open_ratio[2] \n" + 
				"	,day_open_ratio[3] \n" + 
				"	,day_open_ratio[4] \n" + 
				"	,day_open_ratio[5] \n" + 
				"	,day_open_ratio[6] \n" + 
				"	,day_open_ratio[7] \n" + 
				"	,day_open_ratio[8] \n" + 
				"	,day_open_ratio[9] \n" + 
				"	,day_open_ratio[10] \n" + 
				"	,day_open_ratio[11] \n" + 
				"	,day_open_ratio[12] \n" + 
				"	,day_open_ratio[13] \n" + 
				"	,day_open_ratio[14] \n" + 
				"	,day_open_ratio[15] \n" + 
				"	,day_open_ratio[16] \n" + 
				"	,day_open_ratio[17] \n" + 
				"	,day_open_ratio[18] \n" + 
				"	,day_open_ratio[19] \n" + 
				"	,day_open_ratio[20] \n" + 
				"	,day_open_ratio[21] \n" + 
				"	,day_open_ratio[22] \n" + 
				"	,day_open_ratio[23] \n" + 
				"	,day_open_ratio[24] \n" + 
				"	,day_open_ratio[25] \n" + 
				"	,day_open_ratio[26] \n" + 
				"	,day_open_ratio[27] \n" + 
				"	,day_open_ratio[28] \n" + 
				"	,day_open_ratio[29] \n" + 
				"	,day_open_ratio[30] \n" + 
				"	,day_open_ratio[31] \n" + 
				"	,day_open_ratio[32] \n" + 
				"	,day_open_ratio[33] \n" + 
				"	,day_open_ratio[34] \n" + 
				"	,day_open_ratio[35] \n" + 
				"	,day_open_ratio[36] \n" + 
				"	,day_open_ratio[37] \n" + 
				"	,day_open_ratio[38] \n" + 
				"	,day_open_ratio[39] \n" + 
				"	,day_open_ratio[40] \n" + 
				"	,day_open_ratio[41] \n" + 
				"	,day_open_ratio[42] \n" + 
				"	,day_open_ratio[43] \n" + 
				"	,day_open_ratio[44] \n" + 
				"	,day_open_ratio[45] \n" + 
				"	,day_open_ratio[46] \n" + 
				"	,day_open_ratio[47] \n" + 
				"	,day_open_ratio[48] \n" + 
				"	,day_open_ratio[49] \n" + 
				"	,day_open_ratio[50] \n" + 
				"	,day_open_ratio[51] \n" + 
				"	,day_open_ratio[52] \n" + 
				"	,day_open_ratio[53] \n" + 
				"	,day_open_ratio[54] \n" + 
				"	,day_open_ratio[55] \n" + 
				"	,day_open_ratio[56] \n" + 
				"	,day_open_ratio[57] \n" + 
				"	,day_open_ratio[58] \n" + 
				"	,day_open_ratio[59] \n" + 
				"	,day_open_ratio[60] \n" + 
				"	,day_low_ratio[1] \n" + 
				"	,day_low_ratio[2] \n" + 
				"	,day_low_ratio[3] \n" + 
				"	,day_low_ratio[4] \n" + 
				"	,day_low_ratio[5] \n" + 
				"	,day_low_ratio[6] \n" + 
				"	,day_low_ratio[7] \n" + 
				"	,day_low_ratio[8] \n" + 
				"	,day_low_ratio[9] \n" + 
				"	,day_low_ratio[10] \n" + 
				"	,day_low_ratio[11] \n" + 
				"	,day_low_ratio[12] \n" + 
				"	,day_low_ratio[13] \n" + 
				"	,day_low_ratio[14] \n" + 
				"	,day_low_ratio[15] \n" + 
				"	,day_low_ratio[16] \n" + 
				"	,day_low_ratio[17] \n" + 
				"	,day_low_ratio[18] \n" + 
				"	,day_low_ratio[19] \n" + 
				"	,day_low_ratio[20] \n" + 
				"	,day_low_ratio[21] \n" + 
				"	,day_low_ratio[22] \n" + 
				"	,day_low_ratio[23] \n" + 
				"	,day_low_ratio[24] \n" + 
				"	,day_low_ratio[25] \n" + 
				"	,day_low_ratio[26] \n" + 
				"	,day_low_ratio[27] \n" + 
				"	,day_low_ratio[28] \n" + 
				"	,day_low_ratio[29] \n" + 
				"	,day_low_ratio[30] \n" + 
				"	,day_low_ratio[31] \n" + 
				"	,day_low_ratio[32] \n" + 
				"	,day_low_ratio[33] \n" + 
				"	,day_low_ratio[34] \n" + 
				"	,day_low_ratio[35] \n" + 
				"	,day_low_ratio[36] \n" + 
				"	,day_low_ratio[37] \n" + 
				"	,day_low_ratio[38] \n" + 
				"	,day_low_ratio[39] \n" + 
				"	,day_low_ratio[40] \n" + 
				"	,day_low_ratio[41] \n" + 
				"	,day_low_ratio[42] \n" + 
				"	,day_low_ratio[43] \n" + 
				"	,day_low_ratio[44] \n" + 
				"	,day_low_ratio[45] \n" + 
				"	,day_low_ratio[46] \n" + 
				"	,day_low_ratio[47] \n" + 
				"	,day_low_ratio[48] \n" + 
				"	,day_low_ratio[49] \n" + 
				"	,day_low_ratio[50] \n" + 
				"	,day_low_ratio[51] \n" + 
				"	,day_low_ratio[52] \n" + 
				"	,day_low_ratio[53] \n" + 
				"	,day_low_ratio[54] \n" + 
				"	,day_low_ratio[55] \n" + 
				"	,day_low_ratio[56] \n" + 
				"	,day_low_ratio[57] \n" + 
				"	,day_low_ratio[58] \n" + 
				"	,day_low_ratio[59] \n" + 
				"	,day_low_ratio[60] \n" + 
				"	,day_high_ratio[1] \n" + 
				"	,day_high_ratio[2] \n" + 
				"	,day_high_ratio[3] \n" + 
				"	,day_high_ratio[4] \n" + 
				"	,day_high_ratio[5] \n" + 
				"	,day_high_ratio[6] \n" + 
				"	,day_high_ratio[7] \n" + 
				"	,day_high_ratio[8] \n" + 
				"	,day_high_ratio[9] \n" + 
				"	,day_high_ratio[10] \n" + 
				"	,day_high_ratio[11] \n" + 
				"	,day_high_ratio[12] \n" + 
				"	,day_high_ratio[13] \n" + 
				"	,day_high_ratio[14] \n" + 
				"	,day_high_ratio[15] \n" + 
				"	,day_high_ratio[16] \n" + 
				"	,day_high_ratio[17] \n" + 
				"	,day_high_ratio[18] \n" + 
				"	,day_high_ratio[19] \n" + 
				"	,day_high_ratio[20] \n" + 
				"	,day_high_ratio[21] \n" + 
				"	,day_high_ratio[22] \n" + 
				"	,day_high_ratio[23] \n" + 
				"	,day_high_ratio[24] \n" + 
				"	,day_high_ratio[25] \n" + 
				"	,day_high_ratio[26] \n" + 
				"	,day_high_ratio[27] \n" + 
				"	,day_high_ratio[28] \n" + 
				"	,day_high_ratio[29] \n" + 
				"	,day_high_ratio[30] \n" + 
				"	,day_high_ratio[31] \n" + 
				"	,day_high_ratio[32] \n" + 
				"	,day_high_ratio[33] \n" + 
				"	,day_high_ratio[34] \n" + 
				"	,day_high_ratio[35] \n" + 
				"	,day_high_ratio[36] \n" + 
				"	,day_high_ratio[37] \n" + 
				"	,day_high_ratio[38] \n" + 
				"	,day_high_ratio[39] \n" + 
				"	,day_high_ratio[40] \n" + 
				"	,day_high_ratio[41] \n" + 
				"	,day_high_ratio[42] \n" + 
				"	,day_high_ratio[43] \n" + 
				"	,day_high_ratio[44] \n" + 
				"	,day_high_ratio[45] \n" + 
				"	,day_high_ratio[46] \n" + 
				"	,day_high_ratio[47] \n" + 
				"	,day_high_ratio[48] \n" + 
				"	,day_high_ratio[49] \n" + 
				"	,day_high_ratio[50] \n" + 
				"	,day_high_ratio[51] \n" + 
				"	,day_high_ratio[52] \n" + 
				"	,day_high_ratio[53] \n" + 
				"	,day_high_ratio[54] \n" + 
				"	,day_high_ratio[55] \n" + 
				"	,day_high_ratio[56] \n" + 
				"	,day_high_ratio[57] \n" + 
				"	,day_high_ratio[58] \n" + 
				"	,day_high_ratio[59] \n" + 
				"	,day_high_ratio[60] \n" + 
				"	,day_close_ratio[1] \n" + 
				"	,day_close_ratio[2] \n" + 
				"	,day_close_ratio[3] \n" + 
				"	,day_close_ratio[4] \n" + 
				"	,day_close_ratio[5] \n" + 
				"	,day_close_ratio[6] \n" + 
				"	,day_close_ratio[7] \n" + 
				"	,day_close_ratio[8] \n" + 
				"	,day_close_ratio[9] \n" + 
				"	,day_close_ratio[10] \n" + 
				"	,day_close_ratio[11] \n" + 
				"	,day_close_ratio[12] \n" + 
				"	,day_close_ratio[13] \n" + 
				"	,day_close_ratio[14] \n" + 
				"	,day_close_ratio[15] \n" + 
				"	,day_close_ratio[16] \n" + 
				"	,day_close_ratio[17] \n" + 
				"	,day_close_ratio[18] \n" + 
				"	,day_close_ratio[19] \n" + 
				"	,day_close_ratio[20] \n" + 
				"	,day_close_ratio[21] \n" + 
				"	,day_close_ratio[22] \n" + 
				"	,day_close_ratio[23] \n" + 
				"	,day_close_ratio[24] \n" + 
				"	,day_close_ratio[25] \n" + 
				"	,day_close_ratio[26] \n" + 
				"	,day_close_ratio[27] \n" + 
				"	,day_close_ratio[28] \n" + 
				"	,day_close_ratio[29] \n" + 
				"	,day_close_ratio[30] \n" + 
				"	,day_close_ratio[31] \n" + 
				"	,day_close_ratio[32] \n" + 
				"	,day_close_ratio[33] \n" + 
				"	,day_close_ratio[34] \n" + 
				"	,day_close_ratio[35] \n" + 
				"	,day_close_ratio[36] \n" + 
				"	,day_close_ratio[37] \n" + 
				"	,day_close_ratio[38] \n" + 
				"	,day_close_ratio[39] \n" + 
				"	,day_close_ratio[40] \n" + 
				"	,day_close_ratio[41] \n" + 
				"	,day_close_ratio[42] \n" + 
				"	,day_close_ratio[43] \n" + 
				"	,day_close_ratio[44] \n" + 
				"	,day_close_ratio[45] \n" + 
				"	,day_close_ratio[46] \n" + 
				"	,day_close_ratio[47] \n" + 
				"	,day_close_ratio[48] \n" + 
				"	,day_close_ratio[49] \n" + 
				"	,day_close_ratio[50] \n" + 
				"	,day_close_ratio[51] \n" + 
				"	,day_close_ratio[52] \n" + 
				"	,day_close_ratio[53] \n" + 
				"	,day_close_ratio[54] \n" + 
				"	,day_close_ratio[55] \n" + 
				"	,day_close_ratio[56] \n" + 
				"	,day_close_ratio[57] \n" + 
				"	,day_close_ratio[58] \n" + 
				"	,day_close_ratio[59] \n" + 
				"	,day_close_ratio[60] \n" + 
				"	,week_open_ratio[1] \n" + 
				"	,week_open_ratio[2] \n" + 
				"	,week_open_ratio[3] \n" + 
				"	,week_open_ratio[4] \n" + 
				"	,week_open_ratio[5] \n" + 
				"	,week_open_ratio[6] \n" + 
				"	,week_open_ratio[7] \n" + 
				"	,week_open_ratio[8] \n" + 
				"	,week_open_ratio[9] \n" + 
				"	,week_open_ratio[10] \n" + 
				"	,week_open_ratio[11] \n" + 
				"	,week_open_ratio[12] \n" + 
				"	,week_open_ratio[13] \n" + 
				"	,week_open_ratio[14] \n" + 
				"	,week_open_ratio[15] \n" + 
				"	,week_open_ratio[16] \n" + 
				"	,week_open_ratio[17] \n" + 
				"	,week_open_ratio[18] \n" + 
				"	,week_open_ratio[19] \n" + 
				"	,week_open_ratio[20] \n" + 
				"	,week_open_ratio[21] \n" + 
				"	,week_open_ratio[22] \n" + 
				"	,week_open_ratio[23] \n" + 
				"	,week_open_ratio[24] \n" + 
				"	,week_open_ratio[25] \n" + 
				"	,week_open_ratio[26] \n" + 
				"	,week_open_ratio[27] \n" + 
				"	,week_open_ratio[28] \n" + 
				"	,week_open_ratio[29] \n" + 
				"	,week_open_ratio[30] \n" + 
				"	,week_open_ratio[31] \n" + 
				"	,week_open_ratio[32] \n" + 
				"	,week_open_ratio[33] \n" + 
				"	,week_open_ratio[34] \n" + 
				"	,week_open_ratio[35] \n" + 
				"	,week_open_ratio[36] \n" + 
				"	,week_low_ratio[1] \n" + 
				"	,week_low_ratio[2] \n" + 
				"	,week_low_ratio[3] \n" + 
				"	,week_low_ratio[4] \n" + 
				"	,week_low_ratio[5] \n" + 
				"	,week_low_ratio[6] \n" + 
				"	,week_low_ratio[7] \n" + 
				"	,week_low_ratio[8] \n" + 
				"	,week_low_ratio[9] \n" + 
				"	,week_low_ratio[10] \n" + 
				"	,week_low_ratio[11] \n" + 
				"	,week_low_ratio[12] \n" + 
				"	,week_low_ratio[13] \n" + 
				"	,week_low_ratio[14] \n" + 
				"	,week_low_ratio[15] \n" + 
				"	,week_low_ratio[16] \n" + 
				"	,week_low_ratio[17] \n" + 
				"	,week_low_ratio[18] \n" + 
				"	,week_low_ratio[19] \n" + 
				"	,week_low_ratio[20] \n" + 
				"	,week_low_ratio[21] \n" + 
				"	,week_low_ratio[22] \n" + 
				"	,week_low_ratio[23] \n" + 
				"	,week_low_ratio[24] \n" + 
				"	,week_low_ratio[25] \n" + 
				"	,week_low_ratio[26] \n" + 
				"	,week_low_ratio[27] \n" + 
				"	,week_low_ratio[28] \n" + 
				"	,week_low_ratio[29] \n" + 
				"	,week_low_ratio[30] \n" + 
				"	,week_low_ratio[31] \n" + 
				"	,week_low_ratio[32] \n" + 
				"	,week_low_ratio[33] \n" + 
				"	,week_low_ratio[34] \n" + 
				"	,week_low_ratio[35] \n" + 
				"	,week_low_ratio[36] \n" + 
				"	,week_high_ratio[1] \n" + 
				"	,week_high_ratio[2] \n" + 
				"	,week_high_ratio[3] \n" + 
				"	,week_high_ratio[4] \n" + 
				"	,week_high_ratio[5] \n" + 
				"	,week_high_ratio[6] \n" + 
				"	,week_high_ratio[7] \n" + 
				"	,week_high_ratio[8] \n" + 
				"	,week_high_ratio[9] \n" + 
				"	,week_high_ratio[10] \n" + 
				"	,week_high_ratio[11] \n" + 
				"	,week_high_ratio[12] \n" + 
				"	,week_high_ratio[13] \n" + 
				"	,week_high_ratio[14] \n" + 
				"	,week_high_ratio[15] \n" + 
				"	,week_high_ratio[16] \n" + 
				"	,week_high_ratio[17] \n" + 
				"	,week_high_ratio[18] \n" + 
				"	,week_high_ratio[19] \n" + 
				"	,week_high_ratio[20] \n" + 
				"	,week_high_ratio[21] \n" + 
				"	,week_high_ratio[22] \n" + 
				"	,week_high_ratio[23] \n" + 
				"	,week_high_ratio[24] \n" + 
				"	,week_high_ratio[25] \n" + 
				"	,week_high_ratio[26] \n" + 
				"	,week_high_ratio[27] \n" + 
				"	,week_high_ratio[28] \n" + 
				"	,week_high_ratio[29] \n" + 
				"	,week_high_ratio[30] \n" + 
				"	,week_high_ratio[31] \n" + 
				"	,week_high_ratio[32] \n" + 
				"	,week_high_ratio[33] \n" + 
				"	,week_high_ratio[34] \n" + 
				"	,week_high_ratio[35] \n" + 
				"	,week_high_ratio[36] \n" + 
				"	,week_close_ratio[1] \n" + 
				"	,week_close_ratio[2] \n" + 
				"	,week_close_ratio[3] \n" + 
				"	,week_close_ratio[4] \n" + 
				"	,week_close_ratio[5] \n" + 
				"	,week_close_ratio[6] \n" + 
				"	,week_close_ratio[7] \n" + 
				"	,week_close_ratio[8] \n" + 
				"	,week_close_ratio[9] \n" + 
				"	,week_close_ratio[10] \n" + 
				"	,week_close_ratio[11] \n" + 
				"	,week_close_ratio[12] \n" + 
				"	,week_close_ratio[13] \n" + 
				"	,week_close_ratio[14] \n" + 
				"	,week_close_ratio[15] \n" + 
				"	,week_close_ratio[16] \n" + 
				"	,week_close_ratio[17] \n" + 
				"	,week_close_ratio[18] \n" + 
				"	,week_close_ratio[19] \n" + 
				"	,week_close_ratio[20] \n" + 
				"	,week_close_ratio[21] \n" + 
				"	,week_close_ratio[22] \n" + 
				"	,week_close_ratio[23] \n" + 
				"	,week_close_ratio[24] \n" + 
				"	,week_close_ratio[25] \n" + 
				"	,week_close_ratio[26] \n" + 
				"	,week_close_ratio[27] \n" + 
				"	,week_close_ratio[28] \n" + 
				"	,week_close_ratio[29] \n" + 
				"	,week_close_ratio[30] \n" + 
				"	,week_close_ratio[31] \n" + 
				"	,week_close_ratio[32] \n" + 
				"	,week_close_ratio[33] \n" + 
				"	,week_close_ratio[34] \n" + 
				"	,week_close_ratio[35] \n" + 
				"	,week_close_ratio[36], \n" + 
				"       -- target_open_ratio, \n" + 
				"       -- target_low_ratio, \n" + 
				"       -- target_high_ratio, \n" + 
				"       round(target_close_ratio*1000)/100 as target_ratio_label \n" + 
				"  from tb_result   ";
 
	}

	public static List<String> getTargetCompanies(int limit) {
		ArrayList<String> list = new ArrayList<String>();
		// MC top 500 && Stock Price < 20,000
		String query = "with tb_lastday as ( "+
			"select max(standard_date) last_date "+
			"  from tb_company_stock_daily "+
			" where stock_id = 'A005960' "+
			") " +
			"select * " +
			"  from tb_company_stock_daily, tb_lastday " +
			" where standard_date = last_date " +
			// "   and stock_price < 20000 " +
			" order by market_capital desc " +
			" limit " + limit;
		Connection con = null;
		ResultSet rs = null;
		try {
			con = getConnection();
			rs = con.createStatement().executeQuery(query);
			while(rs.next())
				list.add(rs.getString("stock_id"));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(con!= null) try {con.close(); }catch(Exception e) {}
		}
		return list;
	}
	
	public static String getLastDay() {
		String query = "with tb_lastday as ( "+
				"select max(standard_date) last_date "+
				"  from tb_company_stock_daily "+
				" where stock_id = 'A005960' "+
				") " +
				"select last_date from tb_lastday";
		Connection con = null;
		ResultSet rs = null;
		String lastDay = null;
		try {
			con = getConnection();
			rs = con.createStatement().executeQuery(query);
			rs.next();
			lastDay = rs.getString("last_date");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(con!= null) try {con.close(); }catch(Exception e) {}
		}
		return lastDay;
		
	}
	
	public static List<String> getClosedDay() {
		String query = "select standard_date from tb_closeday";
		Connection con = null;
		ResultSet rs = null;
		List<String> rtn = new ArrayList<String>();
		try {
			con = getConnection();
			rs = con.createStatement().executeQuery(query);
			while(rs.next()) {
				rtn.add(rs.getString(1));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(con!= null) try {con.close(); }catch(Exception e) {}
		}
		return rtn;
	}
	
	final static String INSERT_PREDICT_METRIC = "insert into tb_predict_matrix (base_standard_date, learning_stock_id, predict_target_date" + 
			", predict_stock_id, learn_count, total_count, accuracy, precisions, recall, score" +
			", result00, result01, result02, result10, result11, result12, result20, result21, result22) values " +
			"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) " +
			"on conflict (base_standard_date, learning_stock_id, predict_target_date, predict_stock_id, learn_count) " +
			"do update set total_count = excluded.total_count , accuracy = excluded.accuracy, precisions = excluded.precisions " +
			", recall = excluded.recall, score = excluded.score, result00 = excluded.result00, result01 = excluded.result01, result02 = excluded.result02 " +
			", result10 = excluded.result10, result11 = excluded.result11, result12 = excluded.result12 " +
			", result20 = excluded.result20, result21 = excluded.result21, result22 = excluded.result22";
	
	final static String SELECT_LAST_PREDICT_METRIC = "select base_standard_date, learning_stock_id, predict_target_date " +
			", predict_stock_id, learn_count, score, total_count, accuracy, precisions, recall " +
			", result00, result01, result02, result10, result11, result12, result20, result21, result22 " +
			"from tb_predict_matrix where base_standard_date = ? and learning_stock_id = ?  and predict_target_date = ? and predict_stock_id = ? order by learn_count desc limit 1";
	
	public static void insertPredictMetric(PredictMetric metric) throws SQLException {
		Connection con = null;
		try {
			con = getConnection();
			PreparedStatement stmt = con.prepareStatement(INSERT_PREDICT_METRIC);
			stmt.setString(1, metric.getBaseStandardDate());
			stmt.setString(2, metric.getLearningStockId());
			stmt.setString(3,  metric.getPredictTargetDate());
			stmt.setString(4,  metric.getPredictStockId());
			stmt.setInt(5,  metric.getLearnCount());
			stmt.setInt(6,  metric.getTotalCount());
			stmt.setFloat(7,  metric.getAccuracy());
			stmt.setFloat(8,  metric.getPrecisions());
			stmt.setFloat(9,  metric.getRecall());
			stmt.setFloat(10,  metric.getScore());
			stmt.setInt(11,  metric.getResult00());
			stmt.setInt(12,  metric.getResult01());
			stmt.setInt(13,  metric.getResult02());
			stmt.setInt(14,  metric.getResult10());
			stmt.setInt(15,  metric.getResult11());
			stmt.setInt(16,  metric.getResult12());
			stmt.setInt(17,  metric.getResult20());
			stmt.setInt(18,  metric.getResult21());
			stmt.setInt(19,  metric.getResult22());
			stmt.executeUpdate();
		} finally {
			if(con != null) try { con.close(); } catch (Exception e) {}
		}
	}
	
	public static void appendPredictMetric(PredictMetric metric) throws SQLException {
		PredictMetric oldOne = new PredictMetric();
		oldOne.setBaseStandardDate(metric.getBaseStandardDate());
		oldOne.setLearningStockId(metric.getLearningStockId());
		oldOne.setPredictTargetDate(metric.getPredictTargetDate());
		oldOne.setPredictStockId(metric.getPredictStockId());
		oldOne = getLastPredictMetric(oldOne);
		if(oldOne != null) {
			metric.setLearnCount(oldOne.getLearnCount() + 1);
		}
		insertPredictMetric(metric);
	}
	
	public static PredictMetric getLastPredictMetric(PredictMetric metric) throws SQLException {
		Connection con = null;
		PredictMetric oldOne = null;
		try {
			con = getConnection();
			PreparedStatement stmt = con.prepareStatement(SELECT_LAST_PREDICT_METRIC);
			stmt.setString(1, metric.getBaseStandardDate());
			stmt.setString(2, metric.getLearningStockId());
			stmt.setString(3,  metric.getPredictTargetDate());
			stmt.setString(4,  metric.getPredictStockId());
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				oldOne = new PredictMetric();
				oldOne.setLearnCount(rs.getInt("learn_count"));
				oldOne.setTotalCount(rs.getInt("total_count"));
				oldOne.setAccuracy(rs.getFloat("accuracy"));
				oldOne.setPrecisions(rs.getFloat("precisions"));
				oldOne.setRecall(rs.getFloat("recall"));
				oldOne.setScore(rs.getFloat("score"));
				oldOne.setResult00(rs.getInt("result00"));
				oldOne.setResult01(rs.getInt("result01"));
				oldOne.setResult02(rs.getInt("result02"));
				oldOne.setResult10(rs.getInt("result10"));
				oldOne.setResult11(rs.getInt("result11"));
				oldOne.setResult12(rs.getInt("result12"));
				oldOne.setResult20(rs.getInt("result20"));
				oldOne.setResult21(rs.getInt("result21"));
				oldOne.setResult22(rs.getInt("result22"));
			}
		} finally {
			if(con != null) try { con.close(); } catch (Exception e) {}
		}
		return oldOne;
	}
	
	public static ResultSet getDailyStockLearningDataRs(String stockId, String startDate) throws SQLException {
		ResultSet rs = null;
		Connection con = getConnection();
		rs = con.createStatement().executeQuery(getDailyStockLearningDataQuery(stockId, startDate));
		return rs;
	}

}
