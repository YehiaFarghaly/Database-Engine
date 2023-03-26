package storage;

import java.io.IOException;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Vector;

import exceptions.DBAppException;
import search.TableSearch;

public class Table implements Serializable {
	private Vector<String> pagesName;
	private int lastPage;
	private String name, PKColumn;
	private Tuple prototype;
	private Hashtable<String, String> colNameType, colNameMin, colNameMax;

	// we still need to store Pages

	public Table(String name, String PK, Hashtable<String, String> colNameType, Hashtable<String, String> colNameMin,
			Hashtable<String, String> colNameMax) {

		this.name = name;
		this.PKColumn = PK;
		this.colNameType = colNameType;
		this.colNameMin = colNameMin;
		this.colNameMax = colNameMax;
		pagesName = new Vector();

	}

	public Tuple getPrototype() {
		if (this.prototype == null) {
			TupleDirector tupDir = new TupleDirector(new TupleBuilder());
			tupDir.makeTuple(getColNameType());
			this.prototype = tupDir.getTuple();
		}
		return this.prototype.getCopy();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPKColumn() {
		return PKColumn;
	}

	public void setPKColumn(String pKColumn) {
		PKColumn = pKColumn;
	}

	public Hashtable<String, String> getColNameType() {
		return colNameType;
	}

	public void setColNameType(Hashtable<String, String> colNameType) {
		this.colNameType = colNameType;
	}

	public Hashtable<String, String> getColNameMin() {
		return colNameMin;
	}

	public void setColNameMin(Hashtable<String, String> colNameMin) {
		this.colNameMin = colNameMin;
	}

	public Hashtable<String, String> getColNameMax() {
		return colNameMax;
	}

	public void setColNameMax(Hashtable<String, String> colNameMax) {
		this.colNameMax = colNameMax;
	}

	public void insertTuple(Hashtable<String, Object> htblColNameValue) throws DBAppException {

		Tuple tuple = createTuple(htblColNameValue);

		if (isEmptyTable()) {
			
			insertNewPage(tuple);
		}
		else {
			Page page = search(tuple);
			
			if(page.isFull()) {
				
			}
			
			else {
				
				page.insertIntoPage(tuple);
			}
			
		}

	}
	

	public void insertNewPage(Tuple tuple) {
		Page page = new Page(name);
		page.insertIntoPage(tuple);
		pagesName.add(page.getName());
	}

	public boolean isEmptyTable() {
		return pagesName.size() == 0;
	}

	public Tuple createTuple(Hashtable<String, Object> htblColNameValue) {

		Tuple tuple = getPrototype();

		for (Cell c : tuple.getCells()) {

			c.setValue(htblColNameValue.get(c.getKey()));

		}

		return tuple;
	}

	public void insertTupleIntoPage(Tuple tuple) {

	}

	public Vector<Tuple> search(String colName, String value) {
		try {
			return TableSearch.search(this, colName, value);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	public Page search(Tuple t) {
		return null;
	}

	public Vector<Tuple> searchGreaterThan(String colName, String value) {
		try {
			return TableSearch.searchGreaterThan(this, colName, value);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public Vector<Tuple> searchGreaterThanOrEqual(String colName, String value) {
		try {
			return TableSearch.searchGreaterThanOrEqual(this, colName, value);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public Vector<Tuple> searchLessThan(String colName, String value) {
		try {
			return TableSearch.searchLessThan(this, colName, value);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public Vector<Tuple> searchLessThanOrEqual(String colName, String value) {
		try {
			return TableSearch.searchLessThanOrEqual(this, colName, value);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public Vector<Tuple> searchNotEqual(String colName, String value) {
		try {
			return TableSearch.searchNotEqual(this, colName, value);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	public Vector<String> getPagesName() {
		return pagesName;
	}

}
