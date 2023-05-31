package app;

import com.opencsv.exceptions.CsvValidationException;
import constants.Constants;
import datamanipulation.CsvReader;
import datamanipulation.CsvWriter;
import exceptions.DBAppException;
import sql.SQLTerm;
import sql.parser.SQLParser;
import storage.Page;
import storage.Table;
import storage.Tuple;
import storage.index.OctreeIndex;
import util.TypeParser;
import util.filecontroller.Serializer;
import util.search.Selector;
import util.validation.Validator;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * The DBApp class represents a database management system. It implements the
 * IDatabase interface and provides methods for initializing the system,
 * creating tables, creating indices, inserting, updating and deleting records,
 * and selecting records using SQL terms.
 */

public class DBApp implements IDatabase {

	private HashSet<String> myTables;
	private final CsvReader reader;
	private final CsvWriter writer;
	private Object clusteringKey;
	private String clusteringKeyValue;

	public DBApp() {
		this.myTables = new HashSet<>();
		this.writer = new CsvWriter();
		this.reader = new CsvReader();
	}

	public HashSet<String> getMyTables() {
		return myTables;
	}

	public CsvReader getReader() {
		return reader;
	}

	public CsvWriter getWriter() {
		return writer;
	}

	/**
	 * Initializes the database management system by reading all the tables from CSV
	 * file
	 */
	@Override
	public void init() {

		this.myTables = reader.readAllTables();

	}

	/**
	 * Creates a new table in the system with the specified parameters.
	 *
	 * @param strTableName           The name of the table.
	 * @param strClusteringKeyColumn The name of the clustering key column.
	 * @param htblColNameType        A Hashtable containing the name and data type
	 *                               of each column in the table.
	 * @param htblColNameMin         A Hashtable containing the name and minimum
	 *                               value of each column in the table.
	 * @param htblColNameMax         A Hashtable containing the name and maximum
	 *                               value of each column in the table.
	 * @throws DBAppException If the table name is invalid or if the table already
	 *                        exists.
	 * @throws ParseException
	 * @throws IOException    If an error occurs while creating the table files.
	 */
	@Override
	public void createTable(String strTableName, String strClusteringKeyColumn,
			Hashtable<String, String> htblColNameType, Hashtable<String, String> htblColNameMin,
			Hashtable<String, String> htblColNameMax) throws DBAppException {

		Validator.validateTableCreation(myTables, strTableName, strClusteringKeyColumn, htblColNameType, htblColNameMin,
				htblColNameMax);

		Table table = new Table(strTableName, strClusteringKeyColumn, htblColNameType, htblColNameMin, htblColNameMax);
		myTables.add(strTableName);
		writer.write(table);

		table.createTableFiles();
		Serializer.serializeTable(table);

	}

	/**
	 * Inserts a new record into the specified table.
	 *
	 * @param strTableName     The name of the table.
	 * @param htblColNameValue A Hashtable containing the name and value of each
	 *                         column in the record.
	 * @throws DBAppException         If the table name is invalid, the record data
	 *                                is invalid or the record already exists.
	 * @throws CsvValidationException If the record fails CSV validation.
	 * @throws IOException            If an error occurs while inserting the record.
	 * @throws ClassNotFoundException If an error occurs while serializing or
	 *                                deserializing the table.
	 * @throws ParseException         If an error occurs while parsing the record
	 *                                data.
	 */
	@Override
	public void insertIntoTable(String strTableName, Hashtable<String, Object> htblColNameValue) throws DBAppException {

		takeAction(Action.INSERT, strTableName, htblColNameValue);

	}

	/**
	 * Updates a record in a table.
	 *
	 * @param strTableName          the name of the table to update a record in.
	 * @param strClusteringKeyValue the value of the clustering key for the record
	 *                              to be updated.
	 * @param htblColNameValue      the new values for the record.
	 * @throws DBAppException         if there is an error with the database
	 *                                operations.
	 * @throws CsvValidationException if there is an error with the CSV file.
	 * @throws IOException            if there is an error with file operations.
	 * @throws ClassNotFoundException if there is an error with the serialization.
	 * @throws ParseException         if there is an error parsing the input.
	 */
	@Override
	public void updateTable(String strTableName, String strClusteringKeyValue,
			Hashtable<String, Object> htblColNameValue) throws DBAppException {

		this.clusteringKeyValue = strClusteringKeyValue;
		takeAction(Action.UPDATE, strTableName, htblColNameValue);
	}

	/**
	 * Deletes records from a table.
	 *
	 * @param strTableName     the name of the table to delete records from.
	 * @param htblColNameValue the values to match records to be deleted.
	 * @throws DBAppException         if there is an error with the database
	 *                                operations.
	 * @throws CsvValidationException if there is an error with the CSV file.
	 * @throws IOException            if there is an error with file operations.
	 * @throws ClassNotFoundException if there is an error with the serialization.
	 * @throws ParseException         if there is an error parsing the input.
	 */
	@Override
	public void deleteFromTable(String strTableName, Hashtable<String, Object> htblColNameValue) throws DBAppException {

		takeAction(Action.DELETE, strTableName, htblColNameValue);
	}

	/**
	 * Performs an action (insert, delete, or update) on a table.
	 *
	 * @param action           the action to perform.
	 * @param strTableName     the name of the table to perform the action on.
	 * @param htblColNameValue the values to use for the action.
	 * @throws DBAppException         if there is an error with the database
	 *                                operations.
	 * @throws CsvValidationException if there is an error with the CSV file.
	 * @throws IOException            if there is an error with file operations.
	 * @throws ClassNotFoundException if there is an error with the serialization.
	 * @throws ParseException         if there is an error parsing the input.
	 */
	private void takeAction(Action action, String strTableName, Hashtable<String, Object> htblColNameValue)
			throws DBAppException {

		Validator.validateTable(strTableName, myTables);
		Table table = Serializer.deserializeTable(strTableName);

		if (action == Action.INSERT) {

			takeInsertAction(table, htblColNameValue);
		} else if (action == Action.DELETE) {

			takeDeleteAction(table, htblColNameValue);
		} else {

			takeUpdateAction(table, htblColNameValue);
		}
		Serializer.serializeTable(table);

	}

	private void takeInsertAction(Table table, Hashtable<String, Object> htblColNameValue) throws DBAppException {

		Validator.validateInsertionInput(table, htblColNameValue, myTables);
		table.insertTuple(htblColNameValue);
	}

	private void takeDeleteAction(Table table, Hashtable<String, Object> htblColNameValue) throws DBAppException {

		Validator.validateDeletionInput(table, htblColNameValue, myTables);
		table.deleteTuples(htblColNameValue);
	}

	private void takeUpdateAction(Table table, Hashtable<String, Object> htblColNameValue) throws DBAppException {

		castClusteringKeyType(table);
		Validator.checkNoClusteringKey(htblColNameValue, table);
		htblColNameValue.put(table.getPKColumn(), clusteringKey);
		Validator.validateUpdateInput(table, htblColNameValue, myTables);

		if (Validator.foundPK(table, htblColNameValue))
			table.updateRecordsInTaple(clusteringKey, htblColNameValue);
	}

	private void castClusteringKeyType(Table table) {

		clusteringKey = TypeParser.castClusteringKey(table, clusteringKeyValue);
	}

	private static ArrayList<String> fillcolNames(SQLTerm[] arrSQLTerms, int index) {
		ArrayList<String> colNames = new ArrayList<String>();
		for (int i = 0; i < 3; i++) {
			colNames.add(arrSQLTerms[index + i]._strColumnName);
		}
		return colNames;
	}

	public Iterator selectFromTable(SQLTerm[] arrSQLTerms, String[] strarrOperators) throws DBAppException {
		Vector<Vector<Tuple>> result = new Vector<>();
		Validator.validateSelectionInput(arrSQLTerms, strarrOperators, myTables);
		if (strarrOperators.length < 2) {

			return Selector.selectWithNoIndex(arrSQLTerms, strarrOperators).iterator();
		}
		int len = arrSQLTerms.length;
		for (int i = 0; i < len - 1; i++) {

			Table table = Serializer.deserializeTable(arrSQLTerms[0]._strTableName);
			if (i < len - 2 && strarrOperators[i].toLowerCase().equals(Constants.AND_OPERATION)
					&& strarrOperators[i + 1].toLowerCase().equals(Constants.AND_OPERATION)) {
				ArrayList<String> colNames = fillcolNames(arrSQLTerms, i);
				for (OctreeIndex<?> index : table.getIndices()) {
					int idx[] = new int[3];
					idx[0] = colNames.indexOf(index.getColName1());
					idx[1] = colNames.indexOf(index.getColName2());
					idx[2] = colNames.indexOf(index.getColName3());
					if (idx[0] != -1 && idx[1] != -1 && idx[2] != -1) {
						SQLTerm[] arrSQLTermsIndex = new SQLTerm[3];
						String[] columnsNames = new String[3];
						for (int j = 0; j < 3; j++) {
							arrSQLTermsIndex[j] = arrSQLTerms[i + idx[j]];
							columnsNames[j] = colNames.get(idx[j]);
						}

						strarrOperators = removeFromStrarrOperators(strarrOperators, i);
						strarrOperators = removeFromStrarrOperators(strarrOperators, i + 1);
						result.add(Selector.selectWithIndex(index, arrSQLTermsIndex, columnsNames, table));
						i = i + 1;
						break;
					}
				}
			} else {
				Hashtable<String, Object> colNameValue = new Hashtable<>();
				colNameValue.put(arrSQLTerms[i]._strColumnName, arrSQLTerms[i]._objValue);
				result.add(Selector.selectFromTableHelper(arrSQLTerms[i]._strTableName, colNameValue,
						arrSQLTerms[i]._strOperator));
				if (i == strarrOperators.length - 1) {
					colNameValue.put(arrSQLTerms[i + 1]._strColumnName, arrSQLTerms[i + 1]._objValue);
					result.add(Selector.selectFromTableHelper(arrSQLTerms[i + 1]._strTableName, colNameValue,
							arrSQLTerms[i + 1]._strOperator));
				}

			}
		}
		return Selector.applyArrOperators(result, strarrOperators).iterator();

	}

	private String[] removeFromStrarrOperators(String[] strarrOperators, int index) {
		String[] res = new String[strarrOperators.length];
		for (int i = 0; i < strarrOperators.length && i != index; i++) {
			for (int j = 0; j < strarrOperators.length - 1; j++) {
				res[j] = strarrOperators[i];
			}
		}
		return res;
	}

	private HashMap<OctreeIndex<?>, ArrayList<Integer>> getApplicableIndices(
			HashMap<OctreeIndex<?>, ArrayList<Integer>> possibleIndices, String[] opr) {
		HashMap<OctreeIndex<?>, ArrayList<Integer>> applicableIndices = new HashMap<>();
		for (Map.Entry<OctreeIndex<?>, ArrayList<Integer>> index : possibleIndices.entrySet()) {
			int idxSQLTerm1 = index.getValue().get(0);
			int idxSQLTerm2 = index.getValue().get(1);
			int idxSQLTerm3 = index.getValue().get(2);

			if (Math.abs(idxSQLTerm3 - idxSQLTerm2) != 1 || Math.abs(idxSQLTerm3 - idxSQLTerm1) != 2
					|| Math.abs(idxSQLTerm1 - idxSQLTerm2) != 1)
				continue;
			if (opr[idxSQLTerm1].equals(Constants.AND_OPERATION) && opr[idxSQLTerm1].equals(opr[idxSQLTerm2]))
				applicableIndices.put(index.getKey(), index.getValue());
		}
		return applicableIndices;
	}

	private Integer[] chooseIndexIdx(HashMap<OctreeIndex<?>, ArrayList<Integer>> possibleIndices,
			SQLTerm[] arrSQLTerms) {
		ArrayList<Integer> allIndexIdx = new ArrayList<>();

		for (Map.Entry<OctreeIndex<?>, ArrayList<Integer>> index : possibleIndices.entrySet())
			for (int i : index.getValue())
				allIndexIdx.add(i);

		ArrayList<Integer> noIndexIdx = new ArrayList<>();

		for (int i = 0; i < arrSQLTerms.length; i++)
			if (!allIndexIdx.contains(i))
				noIndexIdx.add(i);

		Integer[] arr = new Integer[noIndexIdx.size()];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = noIndexIdx.get(i);
		}
		return arr;
	}

	public Iterator union(Iterator i1, Iterator i2) {
		if (i1 == null && i2 == null)
			return null;
		if (i1 == null)
			return i2;
		if (i2 == null)
			return i1;

		Vector<Tuple> union = new Vector<>();
		while (i1.hasNext())
			union.add((Tuple) i1.next());
		while (i2.hasNext())
			union.add((Tuple) i2.next());

		return union.iterator();
	}

	private HashMap<OctreeIndex<?>, ArrayList<Integer>> getPossibleIndex(SQLTerm[] arrSQLTerms) throws DBAppException {

		if (arrSQLTerms.length < 3)
			return null;
		String table = arrSQLTerms[0]._strTableName;
		ArrayList<String> colNames = new ArrayList<>();
		for (int i = 0; i < arrSQLTerms.length; i++)
			colNames.add(arrSQLTerms[i]._strColumnName);
		Table currTable = Serializer.deserializeTable(table);
		HashMap<OctreeIndex<?>, ArrayList<Integer>> foundIndexIdx = new HashMap<>();
		for (OctreeIndex<?> index : currTable.getIndices()) {
			int idx1 = colNames.indexOf(index.getColName1());
			int idx2 = colNames.indexOf(index.getColName2());
			int idx3 = colNames.indexOf(index.getColName3());
			if (idx1 == -1 || idx2 == -1 || idx3 == -1)
				continue;
			colNames.set(idx1, null);
			colNames.set(idx2, null);
			colNames.set(idx3, null);
			ArrayList<Integer> tmpIdx = new ArrayList<>();
			tmpIdx.add(idx1);
			tmpIdx.add(idx2);
			tmpIdx.add(idx3);
			foundIndexIdx.put(index, tmpIdx);
		}
		currTable = null;
		return foundIndexIdx;
	}

	@Override
	public void createIndex(String strTableName, String[] strarrColName) throws DBAppException {
		Validator.validateTable(strTableName, myTables);
		Table table = Serializer.deserializeTable(strTableName);
		Validator.validateCreatIndex(table, strarrColName);
		OctreeIndex index = new OctreeIndex(strTableName, strarrColName[0], strarrColName[1], strarrColName[2]);
		table.getIndices().add(index);
		if (!table.isEmpty()) {
			insertExisitngTuples(strTableName, index, table);
		}
		String indexName = strarrColName[0] + strarrColName[1] + strarrColName[2] + "Index";
		updateCsvFile(strTableName, indexName, strarrColName);
		Serializer.serializeTable(table);
	}

	private void updateCsvFile(String strTableName, String indexName, String[] strarrColName) throws DBAppException {
		CsvWriter cw = new CsvWriter();
		cw.updateCsvFile(strTableName, indexName, strarrColName);
	}

	private void insertExisitngTuples(String strTableName, OctreeIndex index, Table table) throws DBAppException {
		int numOfPages = table.getPagesName().size();
		for (int i = 0; i < numOfPages; i++) {
			Page page = table.getPageAtPosition(i);
			Vector<Tuple> tuples = page.getTuples();
			for (Tuple tuple : tuples) {
				index.add(page, tuple);
			}
		}
	}

	public Iterator parseSQL(StringBuffer strbufSQL) throws DBAppException {
		SQLParser parser = new SQLParser(this);
		Iterator result = parser.parse(strbufSQL);
		return result;
	}

}