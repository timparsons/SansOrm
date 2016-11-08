package com.zaxxer.sansorm.annotation;

public @interface References {

	/**
	 * The object class that this object references
	 */
	Class<?> type();

	/**
	 * Prefix to append to column name when looking for columns of referenced
	 * type in the result set
	 */
	String prefix();
}
