package net.satoshieguchi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

public class FileUtility {
	protected static class PathContainer implements Comparable<PathContainer> {
		private Path path_ = null;
		private String body_ = "";
		private int length_ = 0;
		
		protected PathContainer() { }
		
		public PathContainer(Path path) {
			this();
			
			path_ = path.toAbsolutePath();
			final String name = path_.getFileName().toString().toLowerCase();
			
			// removes the extension
			final int iext = name.lastIndexOf('.');
			if (iext == 0) {
				throw new InvalidPathException(path_.toString(), "There is no extension.");
			} else if (iext < 0) {
				body_ = name;
			} else {
				body_ = name.substring(0, iext);
			}
			
			length_ = body_.length();
		}
		
		public Path getPath() {
			return path_;
		}
		
		public String getBody() {
			return body_;
		}
		
		public int getLength() {
			return length_;
		}
		
		@Override
		public int compareTo(PathContainer o) {
			// lexical ordering
			return (this == o) ? 0 : body_.compareTo(o.body_);
		}
	}
	
	// PathContainer-s in the results are sorted in descending order.
	protected static NavigableMap<Integer, NavigableSet<PathContainer>> createPathMap(Collection<Path> c) {
		final TreeMap<Integer, NavigableSet<PathContainer>> ret = new TreeMap<Integer, NavigableSet<PathContainer>>();
		
		final Iterator<Path> it = c.iterator();
		while (it.hasNext()) {
			final PathContainer path = new PathContainer(it.next());
			final Integer len = path.getLength();
			NavigableSet<PathContainer> s = ret.get(len);
			if (s == null) {
				s = new TreeSet<PathContainer>(Collections.reverseOrder());
				ret.put(len, s);
			}
			s.add(path);
		}
		
		return ret;
	}
	
	public static class PathRelationContainer implements Cloneable {
		private TreeMap<Path, Path> relation_ = new TreeMap<Path, Path>();
		private TreeSet<Path> notFound1_ = new TreeSet<Path>();
		private TreeSet<Path> notFound2_ = new TreeSet<Path>();
		
		public PathRelationContainer() { }
		
		public PathRelationContainer(PathRelationContainer c) {
			this();
			set(c);
		}
		
		public void setRelation(Map<Path, Path> m) {
			relation_.clear();
			relation_.putAll(m);
		}
		
		public SortedMap<Path, Path> getRelation() {
			return relation_;
		}
		
		public void setNotFound1(Collection<Path> c) {
			notFound1_.clear();
			notFound1_.addAll(c);
		}
		
		public SortedSet<Path> getNotFound1() {
			return notFound1_;
		}
		
		public void setNotFound2(Collection<Path> c) {
			notFound2_.clear();
			notFound2_.addAll(c);
		}
		
		public SortedSet<Path> getNotFound2() {
			return notFound2_;
		}
		
		public void set(PathRelationContainer c) {
			if (c == this) {
				return;
			}
			
			setRelation(c.relation_);
			setNotFound1(c.notFound1_);
			setNotFound2(c.notFound2_);
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public PathRelationContainer clone() throws CloneNotSupportedException {
			PathRelationContainer ret = (PathRelationContainer)(super.clone());
			ret.relation_ = (TreeMap<Path, Path>)(relation_.clone());
			ret.notFound1_ = (TreeSet<Path>)(notFound1_.clone());
			ret.notFound2_ = (TreeSet<Path>)(notFound2_.clone());
			return ret;
		}
	}
	
	protected static SortedSet<Path> flattenPathMap(NavigableMap<Integer, NavigableSet<PathContainer>> m) {
		final TreeSet<Path> ret = new TreeSet<Path>();
		
		final Set<Integer> keySet = m.keySet();
		for (Integer len : keySet) {
			final Set<PathContainer> pcSet = m.get(len);
			for (PathContainer pc : pcSet) {
				ret.add(pc.getPath());
			}
		}
		
		return ret;
	}
	
	// makes relationships between path1 and path2 such as
	//   * abcd (path1) <--> abcd_XXXX (path2)
	public static PathRelationContainer makePathRelation(Collection<Path> path1List, Collection<Path> path2List) {
		final TreeMap<Path, Path> relation = new TreeMap<Path, Path>();
		
		final NavigableMap<Integer, NavigableSet<PathContainer>> map1 = createPathMap(path1List).descendingMap();
		final NavigableMap<Integer, NavigableSet<PathContainer>> map2 = createPathMap(path2List);
		
		final Set<Integer> path1LengthSet = map1.keySet();
		final Iterator<Integer> path1LengthIt = path1LengthSet.iterator();
		while (path1LengthIt.hasNext()) {
			final Integer path1Length = path1LengthIt.next();
			
			final Set<PathContainer> path1Set = map1.get(path1Length);
			
			// candidates have names longer than path1
			final Map<Integer, NavigableSet<PathContainer>> path2Cand = map2.tailMap(path1Length, true).descendingMap();
			
			// searches for the longest element which matches path1
			final Iterator<PathContainer> path1It = path1Set.iterator();
			while (path1It.hasNext()) {
				final PathContainer path1 = path1It.next();
				
				final Set<Integer> path2LengthSet = path2Cand.keySet();
				final Iterator<Integer> path2LengthIt = path2LengthSet.iterator();
				
				SEARCH_LOOP:
				while (path2LengthIt.hasNext()) {
					final Integer path2Length = path2LengthIt.next();
					
					final Set<PathContainer> path2Set = path2Cand.get(path2Length);
					final Iterator<PathContainer> path2It = path2Set.iterator();
					while (path2It.hasNext()) {
						final PathContainer path2 = path2It.next();
						
						final String name1 = path1.getBody();
						final String name2 = path2.getBody();
						
						if (name2.startsWith(name1)) {
							// found
							relation.put(path1.getPath(), path2.getPath());
							
							path1It.remove();
							path2It.remove();
							
							if (path2Set.isEmpty()) {
								path2LengthIt.remove();
							}
							
							break SEARCH_LOOP;
						}
					}
				}
			}
			
			if (path1Set.isEmpty()) {
				path1LengthIt.remove();
			}
		}
		
		final SortedSet<Path> notFound1 = flattenPathMap(map1);
		final SortedSet<Path> notFound2 = flattenPathMap(map2);
		
		final PathRelationContainer ret = new PathRelationContainer();
		ret.setRelation(relation);
		ret.setNotFound1(notFound1);
		ret.setNotFound2(notFound2);
		
		return ret;
	}

	public static List<Path> searchFile(Path dir, String ext) throws IOException {
		final String extLower = ext.toLowerCase();
		final LinkedList<Path> fileList = new LinkedList<Path>();
		
		Predicate<? super Path> doesMatch = p -> {
			if (!Files.isRegularFile(p)) {
				// not regular file
				return false;
			} else {
				final String nameLower = p.getFileName().toString().toLowerCase();
				return nameLower.endsWith(extLower);
			}
		};
		
		Files.walk(dir.toAbsolutePath()).filter(doesMatch).forEach(p -> fileList.add(p));
		
		return fileList;
	}
}
