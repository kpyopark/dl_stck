package com.elevenquest.dl.pipeline;

import java.util.List;

import com.elevenquest.dl.pipeline.dao.DailyStockDao;

public class BaseDataCreator {
	
	public static void main(String[] args) {
		List<String> targetCompanyIds = DailyStockDao.getTargetCompanies(2000);
		String startDate = "20120101";
		//System.out.println(FileUtil.getNextWorkday(lastDate, getClosedDay()));
		for(String stockId : targetCompanyIds) {
			DbToCsvConverter.makeCsv(stockId, startDate, FileUtil.getBaseDataCsvFilePath(startDate, stockId));
		}		
	}
}
