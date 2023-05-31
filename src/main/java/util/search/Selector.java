package util.search;

import constants.Constants;
import datamanipulation.CsvReader;
import exceptions.DBAppException;
import sql.SQLTerm;
import storage.Page;
import storage.Table;
import storage.Tuple;
import storage.index.*;
import storage.index.OctreeIndex;
import util.TypeParser;
import util.filecontroller.Serializer;

import java.util.*;

import static constants.Constants.*;

public class Selector {

	public static Vector<Tuple> selectOperation(Vector<Tuple> set1, Vector<Tuple> set2, String operation) {
		if (operation.equals(Constants.AND_OPERATION))
			return getANDingResults(set1, set2);
		else if (operation.equals(Constants.OR_OPERATION))
			return getORingResults(set1, set2);
		else
			return getXORingResults(set1, set2);
	}

	private static Vector<Tuple> getANDingResults(Vector<Tuple> set1, Vector<Tuple> set2) {
		Vector<Tuple> result = new Vector<>();
		for (int i = 0; i < set1.size(); i++) {
			if (vectorContainsTuple(set2, set1.get(i)))
				result.add(set1.get(i));
		}
		return result;
	}

	private static Vector<Tuple> getORingResults(Vector<Tuple> set1, Vector<Tuple> set2) {
		Vector<Tuple> result = new Vector<>();
		result.addAll(set2);
		for (int i = 0; i < set1.size(); i++) {
			if (!vectorContainsTuple(result, set1.get(i))) {
				result.add(set1.get(i));
			}
		}
		return result;
	}

	private static Vector<Tuple> getXORingResults(Vector<Tuple> set1, Vector<Tuple> set2) {
		Vector<Tuple> andResult = getANDingResults(set1, set2);
		Vector<Tuple> orResult = getORingResults(set1, set2);
		for (int i = 0; i < orResult.size(); i++) {
			if (vectorContainsTuple(andResult, orResult.get(i))) {
				orResult.remove(i--);
			}
		}
		return orResult;
	}

	private static boolean vectorContainsTuple(Vector<Tuple> vector, Tuple tuple) {
		for (Tuple vectorTuple : vector) {
			if (vectorTuple.getPrimaryKey().equals(tuple.getPrimaryKey()))
				return true;
		}
		return false;
	}

	public static Vector<Tuple> selectWithNoIndex(SQLTerm[] arrSQLTerms, String[] strarrOperators)
			throws DBAppException {
		Vector<Vector<Tuple>> result = new Vector<>();
		for (int i = 0; i < arrSQLTerms.length; i++) {
			Hashtable<String, Object> colNameValue = new Hashtable<>();
			colNameValue.put(arrSQLTerms[i]._strColumnName, arrSQLTerms[i]._objValue);
			result.add(selectFromTableHelper(arrSQLTerms[i]._strTableName, colNameValue, arrSQLTerms[i]._strOperator));
		}
		return applyArrOperators(result, strarrOperators);
	}

	private static Hashtable<String, Object> getMax(String tableName, String[] col) {
		Hashtable<String, Object> colMax = new Hashtable<String, Object>();
		CsvReader reader = new CsvReader();
		List<String[]> columns = reader.readTable(tableName);

		for (String[] arr : columns) {
			if (arr[Constants.COLUMN_NAME_INDEX].equals(col[0])) {
				colMax.put(col[0],
						TypeParser.typeParser(arr[Constants.COL_MAX_VALUE_INDEX], arr[Constants.COLUMN_TYPE_INDEX]));
			} else if (arr[Constants.COLUMN_NAME_INDEX].equals(col[1])) {
				colMax.put(col[1],
						TypeParser.typeParser(arr[Constants.COL_MAX_VALUE_INDEX], arr[Constants.COLUMN_TYPE_INDEX]));
			} else if (arr[Constants.COLUMN_NAME_INDEX].equals(col[2])) {
				colMax.put(col[2],
						TypeParser.typeParser(arr[Constants.COL_MAX_VALUE_INDEX], arr[Constants.COLUMN_TYPE_INDEX]));
			}
		}
		return colMax;
	}

	private static Hashtable<String, Object> getMin(String tableName, String[] col) {
		Hashtable<String, Object> colMin = new Hashtable<String, Object>();
		CsvReader reader = new CsvReader();
		List<String[]> columns = reader.readTable(tableName);

		for (String[] arr : columns) {
			if (arr[Constants.COLUMN_NAME_INDEX].equals(col[0])) {
				colMin.put(col[0],
						TypeParser.typeParser(arr[Constants.COL_MIN_VALUE_INDEX], arr[Constants.COLUMN_TYPE_INDEX]));
			} else if (arr[Constants.COLUMN_NAME_INDEX].equals(col[1])) {
				colMin.put(col[1],
						TypeParser.typeParser(arr[Constants.COL_MIN_VALUE_INDEX], arr[Constants.COLUMN_TYPE_INDEX]));
			} else if (arr[Constants.COLUMN_NAME_INDEX].equals(col[2])) {
				colMin.put(col[2],
						TypeParser.typeParser(arr[Constants.COL_MIN_VALUE_INDEX], arr[Constants.COLUMN_TYPE_INDEX]));
			}
		}
		return colMin;
	}

	private static List getPageIndcies(String[] strOperator, OctreeIndex index, String[] ColumnsNames,
			Hashtable<String, Object> colNameValue, String TableName) throws DBAppException {
		Hashtable<String, Object> min = getMin(TableName, ColumnsNames);
		Hashtable<String, Object> max = getMax(TableName, ColumnsNames);
		Object minbounds[] = new Object[3];
		Object maxbounds[] = new Object[3];
		int minmask = 0;
		int maxmask = 0;
		for (int i = 0; i < strOperator.length; i++) {
			if (strOperator[i].equals(Constants.EQUAL)) {
				minbounds[i] = colNameValue.get(ColumnsNames[i]);
				maxbounds[i] = colNameValue.get(ColumnsNames[i]);
			} else if (strOperator[i].equals(Constants.GREATER_THAN_OR_EQUAL)) {
				minbounds[i] = colNameValue.get(ColumnsNames[i]);
				maxbounds[i] = max.get(ColumnsNames[i]);
			} else if (strOperator[i].equals(Constants.GREATER_THAN)) {
				minbounds[i] = colNameValue.get(ColumnsNames[i]);
				maxbounds[i] = max.get(ColumnsNames[i]);
				minmask |= (1 << i);
			} else if (strOperator[i].equals(Constants.LESS_THAN_OR_EQUAL)) {
				minbounds[i] = min.get(ColumnsNames[i]);
				maxbounds[i] = colNameValue.get(ColumnsNames[i]);
			} else if (strOperator[i].equals(Constants.LESS_THAN)) {
				minbounds[i] = min.get(ColumnsNames[i]);
				maxbounds[i] = colNameValue.get(ColumnsNames[i]);
				maxmask |= (1 << i);
			} else if (strOperator[i].equals(Constants.LESS_THAN)) {
				minbounds[i] = min.get(ColumnsNames[i]);
				maxbounds[i] = max.get(ColumnsNames[i]);
			}
		}

		OctreeBounds bounds = new OctreeBounds(minbounds[0], minbounds[1], minbounds[2], maxbounds[0], maxbounds[1],
				maxbounds[2]);
		return index.query(bounds, minmask, maxmask);
	}

	public static HashSet<Integer> getPagesNumbers(List pagespathes, Table table) {
		HashSet<Integer> result = new HashSet<>();
		for (int i = 0; i < pagespathes.size(); i++) {
			result.add(table.getPageIdxFromPath(pagespathes.get(i).toString()));
		}
		return result;
	}

	public static Vector<Tuple> selectWithIndex(OctreeIndex index, SQLTerm[] arrSQLTerms, String[] ColumnsNames,
			Table table) throws DBAppException {
		String[] strarrOperators = new String[2];
		List pagepathes;
		HashSet<Integer> pagenumbers;
		String strOperator[] = new String[arrSQLTerms.length];
		Vector<Vector<Tuple>> result = new Vector<>();
		Hashtable<String, Object> colNameValue = new Hashtable<>();
		for (int i = 0; i < arrSQLTerms.length; i++) {
			colNameValue.put(arrSQLTerms[i]._strColumnName, arrSQLTerms[i]._objValue);
			strOperator[i] = arrSQLTerms[i]._strOperator;
		}
		pagepathes = getPageIndcies(strOperator, index, ColumnsNames, colNameValue, table.getName());
		pagenumbers = getPagesNumbers(pagepathes, table);
		int idx = 0;
		for (String key : colNameValue.keySet()) {
			Hashtable<String, Object> colVal = new Hashtable<>();
			colVal.put(key, colNameValue.get(key));
			result.add(selectFromTableWithIndex(arrSQLTerms[idx]._strTableName, colVal, arrSQLTerms[idx++]._strOperator,
					pagenumbers));
		}
		for (int i = 0; i < arrSQLTerms.length; i++) {

		}

		strarrOperators[0] = Constants.AND_OPERATION;
		strarrOperators[1] = Constants.AND_OPERATION;
		return applyArrOperators(result, strarrOperators);

	}

//    public static Iterator selectWithIndex(SQLTerm[] terms, String[] opr, HashMap<OctreeIndex<?>, ArrayList<Integer>> indIdx) throws DBAppException {
//
//        String tableName = terms[0]._strTableName;
//        Vector<Vector<Tuple>> finalResult = new Vector<>();
//        for (Map.Entry<OctreeIndex<?>, ArrayList<Integer>> curr : indIdx.entrySet()) {
//            ArrayList<Integer> idx = curr.getValue();
//            OctreeIndex currIndex = curr.getKey();
//            OctreeBounds octreeBounds = prepareOctreeBounds(currIndex, idx, terms);
//            List<Object> pages = curr.getKey().query(octreeBounds);
//            Vector<Vector<Tuple>> result = new Vector<>();
//            getPrimarySelectResult(pages, tableName, idx, terms, result);
//            String[] oprBetweenIndexCol = {AND_OPERATION, AND_OPERATION, AND_OPERATION};
//            finalResult.add(applyArrOperators(result, oprBetweenIndexCol));
//        }
//        return finalResult.iterator();
//    }

	private static void getPrimarySelectResult(List<Object> pages, String tableName, ArrayList<Integer> idx,
			SQLTerm[] arrSQLTerms, Vector<Vector<Tuple>> result) throws DBAppException {
		for (Object tmp : pages) {
			String tmpPage = (String) tmp;
			Page page = Serializer.deserializePage(tableName, tmpPage);
			for (int i : idx) // O(3)
				result.add(getResultforIndexSelect(page, i, arrSQLTerms));
		}
	}

	private static Vector<Tuple> getResultforIndexSelect(Page page, int idx, SQLTerm[] terms) {
		Hashtable<String, Object> colNameVal = new Hashtable<>();
		colNameVal.put(terms[idx]._strColumnName, terms[idx]._objValue);
		String operator = terms[idx]._strOperator;
		return page.select(colNameVal, operator);
	}

	private static OctreeBounds prepareOctreeBounds(OctreeIndex<?> index, ArrayList<Integer> idx,
			SQLTerm[] arrSQLTerms) {
		Object[] min = new Object[3];
		Object[] max = new Object[3];

		for (int i : idx) {
			Object[] minmax = prepareMinMax(arrSQLTerms[i]._objValue, arrSQLTerms[i]._strOperator);
			min[i] = minmax[0];
			max[i] = minmax[1];
		}

		return new OctreeBounds(min[0], min[1], min[2], max[0], max[1], max[2]);
	}

	private static Object[] prepareMinMax(Object val, String operator) {

		Object[] minMax = new Object[2]; // 0 for min// 1 for max
		switch (operator) {
		case EQUAL:
			minMax[0] = val;
			minMax[1] = val;
			break;
		case GREATER_THAN:
			minMax[0] = ((Integer) val + 1);
			minMax[1] = Integer.MAX_VALUE;
			break;
		case LESS_THAN:
			minMax[1] = ((Integer) val - 1);
			minMax[0] = Integer.MIN_VALUE;
			break;
		case GREATER_THAN_OR_EQUAL:
			minMax[0] = val;
			minMax[1] = Integer.MAX_VALUE;
			break;
		case LESS_THAN_OR_EQUAL:
			minMax[1] = val;
			minMax[0] = Integer.MIN_VALUE;
			break;
		// SHOULD TO BE CHECKED
		case NOT_EQUAL:
			minMax[0] = Integer.MIN_VALUE;
			minMax[1] = Integer.MAX_VALUE;
		}
		return minMax;
	}

	public static Vector<Tuple> selectFromTableWithIndex(String strTableName, Hashtable<String, Object> colNameValue,
			String operator, HashSet<Integer> pageIndex) throws DBAppException {
		Vector<Tuple> result = new Vector<>();
		Table table = Serializer.deserializeTable(strTableName);
		for (int index : pageIndex) {
			Page page = table.getPageAtPosition(index);
			result.addAll(page.select(colNameValue, operator));
		}
		return result;
	}

	public static Vector<Tuple> selectFromTableHelper(String strTableName, Hashtable<String, Object> colNameValue,
			String operator) throws DBAppException {
		Table table = Serializer.deserializeTable(strTableName);
		return table.select(colNameValue, operator);
	}

	public static Vector<Tuple> applyArrOperators(Vector<Vector<Tuple>> selections, String[] strarrOperators) {

		Vector<Tuple> result = new Vector<>();
		Vector<Vector<Tuple>> tmp = new Vector<>();
		int idx = 0;

		for (Vector<Tuple> tuplesVector : selections) {
			tmp.add(tuplesVector);
			if (tmp.size() == 2) {
				Vector<Tuple> firstSet = tmp.remove(0);
				Vector<Tuple> secondSet = tmp.remove(0);
				tmp.add(Selector.selectOperation(firstSet, secondSet, strarrOperators[idx++].toLowerCase()));
			}
		}
		result = tmp.remove(0);

		return result;
	}
}