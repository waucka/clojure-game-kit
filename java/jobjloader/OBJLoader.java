package jobjloader;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import java.io.BufferedReader;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

class Coord2 {
    public float X;
    public float Y;
    public Coord2(float x, float y) {
	X = x;
	Y = y;
    }
}

class Coord3 {
    public float X;
    public float Y;
    public float Z;
    public Coord3(float x, float y, float z) {
	X = x;
	Y = y;
	Z = z;
    }
}

class CoordPair {
    public Coord3 vtx;
    public Coord2 txc;
    public int idx;
    public CoordPair(Coord3 vtx, Coord2 txc, int idx) {
	this.vtx = vtx;
	this.txc = txc;
	this.idx = idx;
    }
}

public class OBJLoader {
    private ArrayList<Coord3> _vtx;
    private ArrayList<Coord2> _txc;
    private ArrayList<CoordPair> _pairs;
    private HashMap<String, CoordPair> _pairMap;
    private ArrayList<Integer> _idx;
    private String _mtllib;
    private String _name;
    private int _nextIdx;

    private static Pattern pat_comment;
    private static Pattern pat_space;

    static {
	pat_comment = Pattern.compile("^#.*");
	pat_space = Pattern.compile("\\p{Space}");
    }

    public OBJLoader() {
	_vtx = new ArrayList<Coord3>();
	_txc = new ArrayList<Coord2>();
	_pairs = new ArrayList<CoordPair>();
	_pairMap = new HashMap<String, CoordPair>();
	_idx = new ArrayList<Integer>();
	_nextIdx = 0;
    }

    public void parse(BufferedReader br) throws Exception {
	String line;
	while ((line = br.readLine()) != null) {
	    //I think I'm in at least the 5th circle of Java Hell now.
	    if (!pat_comment.matcher(line).matches()) {
		String[] lineparts = pat_space.split(line);
		if (lineparts.length > 0) {
		    if (lineparts[0].equals("mtllib")) {
			//Better hope there aren't any spaces in the lib name!
			_mtllib = lineparts[1];
		    } else if (lineparts[0].equals("o")) {
			//Ditto.
			_name = lineparts[1];
		    } else if (lineparts[0].equals("v")) {
			float x = Float.parseFloat(lineparts[1]);
			float y = Float.parseFloat(lineparts[2]);
			float z = Float.parseFloat(lineparts[3]);
			_vtx.add(new Coord3(x, y, z));
		    } else if (lineparts[0].equals("vt")) {
			float x = Float.parseFloat(lineparts[1]);
			float y = Float.parseFloat(lineparts[2]);
			_txc.add(new Coord2(x, y));
		    } else if (lineparts[0].equals("usemtl")) {
			//Ignore for now.  Possibly forever.
		    } else if (lineparts[0].equals("s")) {
			//Ignore for now.  Probably forever.
		    } else if (lineparts[0].equals("f")) {
			if (lineparts.length != 4) {
			    throw new Exception("Not triangles!  Bollocks!");
			}
			for (int i = 1; i < 4; i++) {
			    if (_pairMap.containsKey(lineparts[i])) {
				CoordPair pair = _pairMap.get(lineparts[i]);
				_idx.add(pair.idx);
			    } else {
				String[] parts = lineparts[i].split("/");
				int vtxidx = Integer.parseInt(parts[0]);
				int txcidx = Integer.parseInt(parts[1]);
				CoordPair pair = new CoordPair(_vtx.get(vtxidx - 1), _txc.get(txcidx - 1), _nextIdx);
				_nextIdx++;
				_pairMap.put(lineparts[i], pair);
				_pairs.add(pair);
				_idx.add(pair.idx);
			    }
			}
		    }
		}
	    }
	}
    }

    public float[] getVertices() {
	float[] vtx = new float[_pairs.size() * 3];
	int i = 0;
	for (CoordPair pair : _pairs) {
	    vtx[i + 0] = pair.vtx.X;
	    vtx[i + 1] = pair.vtx.Y;
	    vtx[i + 2] = pair.vtx.Z;
	    i += 3;
	}
	return vtx;
    }

    public float[] getTexCoords() {
	float[] txc = new float[_pairs.size() * 2];
	int i = 0;
	for (CoordPair pair : _pairs) {
	    txc[i + 0] = pair.txc.X;
	    txc[i + 1] = pair.txc.Y;
	    i += 2;
	}
	return txc;
    }

    public int[]  getIndices() {
	int[] idx = new int[_idx.size()];
	int i = 0;
	for (Integer idxval : _idx) {
	    idx[i] = idxval;
	    i++;
	}
	return idx;
    }
}
