package com.elevenquest.dl.pipeline;

import java.util.List;

import com.elevenquest.dl.pipeline.dao.DailyStockDao;

public class BaseDataCreator {
	
	public static void main(String[] args) {
		List<String[]> targetCompanyIds = DailyStockDao.getLearningTargetCompanies(200);
		String startDate = "20120101";
		String targetStockId = null; //"A000020";
		//System.out.println(FileUtil.getNextWorkday(lastDate, getClosedDay()));
		if (targetStockId != null) {
			DbToCsvConverter.makeCsv(targetStockId, startDate, FileUtil.getBaseDataCsvFilePath(startDate, targetStockId));
		} else {
			for(String[] stockIdAndName : targetCompanyIds) {
				DbToCsvConverter.makeCsv(stockIdAndName[0], startDate, FileUtil.getBaseDataCsvFilePath(startDate, stockIdAndName[0]));
			}		
		}
	}
}
