/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.nomi.cwg;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author onovy
 */
public abstract class Import {
	protected InputStream input;

	public Import(InputStream input) {
		this.input = input;
	}

	public abstract void importData(DatabaseAdapter db) throws IOException;

}
