import java.util.*;
import java.io.*;


public class NLP008
{

	public static void main(String[] args) throws IOException
	{

		BufferedWriter writer = new BufferedWriter(new FileWriter("poems.txt"));

		// create a new text processor for the selected text file
		TextProcessor source = new TextProcessor("houndTagged.txt");

		// read text from file, storing tokens, POS tags and syllable counts in respective lists
		source.readSourceText();

		// word associations list
		WordAssociations wan = new WordAssociations("WANs.txt");

		// syllable limits - upper limits
		int limit13 = 8;
		int limit2 = 6;

		// word association tree, root node
		KeywordTree tree = new KeywordTree(new Node(null), limit13, limit2);

		// grow the association tree from the given lists and text
		tree.grow(readKigo("Kigo.txt"), wan, source);



		// remove any imcomplete paths
		tree.prune();

		System.out.println("********************TESTING TREE PRUNING************");
		System.out.println("ROOT: " + tree.getRoot().getLabel());
		for(int i=0; i<tree.getRoot().getChildren().size(); i++)
		{
			System.out.println("***" + tree.getRoot().getChildren().get(i).getLabel());
			for(int j=0; j<tree.getRoot().getChildren().get(i).getChildren().size(); j++)
			{
				System.out.println("******" + tree.getRoot().getChildren().get(i).getChildren().get(j).getLabel());
				for(int k=0; k<tree.getRoot().getChildren().get(i).getChildren().get(j).getChildren().size(); k++)
				{
					System.out.println("*********" + tree.getRoot().getChildren().get(i).getChildren().get(j).getChildren().get(k).getLabel());
				}
			}
		}


		System.out.println("********************TESTING LINES************");
		System.out.println("ROOT: " + tree.getRoot().getLabel());
		// for each level one node
		for(int n1=0; n1<tree.getRoot().getChildren().size(); n1++)
		{
			// for each line of this node
			for(int l1=0; l1<tree.getRoot().getChildren().get(n1).getLines().size(); l1++)
			{
				// for each level two node
				for(int n2=0; n2<tree.getRoot().getChildren().get(n1).getChildren().size(); n2++)
				{
					// for each level two line
					for(int l2=0; l2<tree.getRoot().getChildren().get(n1).getChildren().get(n2).getLines().size(); l2++)
					{
						// for each level three node
						for(int n3=0; n3<tree.getRoot().getChildren().get(n1).getChildren().get(n2).getChildren().size(); n3++)
						{
							for(int l3=0; l3<tree.getRoot().getChildren().get(n1).getChildren().get(n2).getChildren().get(n3).getLines().size(); l3++)
							{
								/*System.out.println(tree.getRoot().getChildren().get(n1).getLines().get(l1));
								System.out.println(tree.getRoot().getChildren().get(n1).getChildren().get(n2).getLines().get(l2));
								System.out.println(tree.getRoot().getChildren().get(n1).getChildren().get(n2).getChildren().get(n3).getLines().get(l3));
								System.out.println("*****************************************");*/
								writer.write(tree.getRoot().getChildren().get(n1).getLines().get(l1));
								writer.newLine();
								writer.write(tree.getRoot().getChildren().get(n1).getChildren().get(n2).getLines().get(l2));
								writer.newLine();
								writer.write(tree.getRoot().getChildren().get(n1).getChildren().get(n2).getChildren().get(n3).getLines().get(l3));
								writer.newLine();
								writer.write("**********************************************");
								writer.newLine();
							}
						}
					}
				}
			}
		}
	}


	// read kigo list from file, line at a time
	// result: one keyword per list entry
	public static ArrayList<String> readKigo(String file) throws IOException
	{
		String l;
		ArrayList<String> k = new ArrayList<String>();
		try (BufferedReader reader = new BufferedReader(new FileReader(file)))
		{

		    while((l = reader.readLine()) != null)
		    {
				// store one word pair per index
				k.add(l);
			}
		}
		return k;
	}
}

// reads text from file
// splits it into lists of tokens
// and corresponding lists of POS tags and syllables
class TextProcessor
{
	private String file;
	private ArrayList<String> tokens;
	private ArrayList<String> tags;
	private ArrayList<Integer> syllables;
	private ArrayList<String>[] chunks;

	public TextProcessor(String f)
	{
		this.file = f;
		tokens = new ArrayList<String>();
		tags = new ArrayList<String>();
		syllables = new ArrayList<Integer>();
		chunks = new ArrayList<String>[5];
		chunks = {"DT", "JJ", "NN","NN", "NN"},{"VB", "PP", "DT", "JJ", "NN"},{"DT", "NN"},{"NN", "PP", "NN"}};
	}

	// getters setters
	public ArrayList<String> getTokens()
	{
		return this.tokens;
	}

	public ArrayList<String> getTags()
	{
		return this.tags;
	}

	public ArrayList<Integer> getSyllables()
	{
		return this.syllables;
	}

	// read source text from file, a character at a time
	public void readSourceText() throws IOException
	{
		String s = "";
		int l;
		try (BufferedReader reader = new BufferedReader(new FileReader(this.file)))
		{
			while((l = reader.read()) != -1)
		    {
				// if / encountered, add the string to tokens and flush temporary string
				if((char)l == '/')
				{
					this.tokens.add(s.toLowerCase());
					this.syllables.add(this.countSyllables(s));
					s = "";
				}
				// if space or newline encountered, add the string to tags and flush temporary string
				else if((char)l == ' ' || l == 10)
				{
					//this.tags.add(s);
					s = "";
				}
				// if none of the above, continue growing the string
				else
				{
					// concatenate characters into a string
					s = s + (char)l;
				}
			}
		}
	}

	// count syllables in a word
	public int countSyllables(String word)
	{
		int syl = 0;
		for(int i=0; i<word.length(); i++)
		{
			if(isVowel(word.charAt(i)))
			{
				if(i>0 && isVowel(word.charAt(i-1)))
				{
					// the vowel is part of a diphthong or a triphthong and we will have counted its predecessor's
					// contribution to the syllable
				}
				else
				{
					// the vowel is e
					if(word.charAt(i) == 'e')
					{
						if(i == word.length()-1 && syl>0)
						{
							// if e is the last letter and there is already at least one syllable in the word
							// then e is mute
							break;
						}
						// if e is the penultimate letter, we look at the surroundings
						else if(i == word.length()-2)
						{
							if(word.charAt(i+1) == 's' && !(word.charAt(i-1) == 'c' || word.charAt(i-1) == 'h' ||
							   word.charAt(i-1) == 's' || word.charAt(i-1) == 'x'))
							{
								// if e is followed by s and not preceded by c, h, s or x
								// then it's mute
								break;
							}
							else if(word.charAt(i+1) == 'd' && !(word.charAt(i-1) == 'd') && !(word.charAt(i-1) == 't'))
							{

								// if e is followed by d and not preceded by d or t
								// then it's mute
								break;
							}
							else
							{
								syl++;
							}
						}
						else
						{
							syl++;
						}
					}
					// if any other vowel but e
					else
					{
						syl++;
					}
				}
			}
		}
		return syl;
	}

	// returns true if the character is a vowel
	public boolean isVowel(char c)
	{
		return (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u');
	}

	// returns locations of all occurences of the word in text
	public ArrayList<Integer> getLocations(String s)
	{
		ArrayList<Integer> locs = new ArrayList<Integer>();

		// iterate over the token list and save indices of the word
		for(int i=0; i<this.getTokens().size(); i++)
		{
			if(this.getTokens().get(i).equals(s))
			{
				locs.add(i);
			}
		}

		return locs;
	}

	public ArrayList<String> fetchLines(String s, int limit)
	{
		ArrayList<String> lines = new ArrayList<String>();
		// try to assemble lines using its context
		ArrayList<Integer> locations = this.getLocations(s);
		for(int i=0; i<locations.size(); i++)
		{
			String line = s;
			int index = locations.get(i);



		}

		// each line is longer than needed for the final version ***NOT TRUE RIGHT NOW***
		return lines;
	}

	/*public ArrayList<String> chunk(int location)
	{

	}*/
}

// the association tree
// contains a list of Keyword Node objects
class KeywordTree
{
	private Node root;
	private int limit13;
	private int limit2;

	public KeywordTree(Node r, int lim13, int lim2)
	{
		this.root = r;
		this.limit13 = lim13;
		this.limit2 = lim2;
	}

	//root getter
	public Node getRoot()
	{
		return this.root;
	}

	// grow the tree from list of keywords and WANs
	public void grow(ArrayList<String> kigo, WordAssociations wan, TextProcessor text)
	{
		NodeFilter filter = new NodeFilter();

		// compare kigo words to source text and to WANs list
		// remove a word if it's not found in either source text or WAN list
		ArrayList<String> levelOne = filter.crossList(kigo,text.getTokens());
		/***************************System.out.println("********************TESTING KIGO VS TEXT************");
		/***************************for(int x=0; x<levelOne.size(); x++)
		/***************************{
		/***************************	System.out.println("LEVEL-ONE: " + levelOne.get(x));
		/***************************}
		/***************************System.out.println("********************************");*/

		levelOne = filter.crossList(levelOne,wan.getCues());

		/***************************System.out.println("********************TESTING KIGO VS WAN************");
		/***************************for(int x=0; x<levelOne.size(); x++)
		/***************************{
		/***************************	System.out.println("LEVEL-ONE: " + levelOne.get(x));
		/***************************}
		/***************************System.out.println("********************************");*/


		// IF WE HAVE COME UP WITH NOTHING AT ALL
		if(levelOne.isEmpty())
		{
			System.out.println("****************CRITICAL FAILURE!!!*********");
		}
		else
		{
			// populate level two
			ArrayList<String> levelTwo = new ArrayList<String>();
			// for every level one entry
			for(int i=0; i<levelOne.size(); i++)
			{
				// connect it to the root
				root.addChild(new Node(levelOne.get(i)));
				root.getChildren().get(i).setLines(text, this.limit13);

				// find children candidates on WAN list
				levelTwo = wan.matchTargets(levelOne.get(i));

				/***************************	System.out.println("********************TESTING LEVEL TWO ASSOCIATIONS************");
				/***************************System.out.println(levelTwo.size());
				/***************************System.out.println(levelOne.get(i) + " LINKS TO:");
				/***************************for(int x=0; x<levelTwo.size(); x++)
				/***************************{
				/***************************	System.out.println("LEVEL-TWO: " + levelTwo.get(x));
				/***************************}
				/***************************System.out.println("********************************");*/

				levelTwo = filter.crossList(levelTwo,kigo);

				// filter out any words not found in source text or as WAN cue word
				levelTwo = filter.crossList(levelTwo,text.getTokens());

				/***************************System.out.println("********************TESTING LEVEL TWO VS TEXT************");
				/***************************System.out.println(levelOne.get(i) + " LINKS TO:");
				/***************************for(int x=0; x<levelTwo.size(); x++)
				/***************************{
				/***************************	System.out.println("LEVEL-TWO: " + levelTwo.get(x));
				/***************************}
				/***************************System.out.println("********************************");*/

				levelTwo = filter.crossList(levelTwo,wan.getCues());

				/***************************System.out.println("********************TESTING LEVEL TWO VS WAN************");
				/***************************System.out.println(levelOne.get(i) + " LINKS TO:");
				/***************************for(int x=0; x<levelTwo.size(); x++)
				/***************************{
				/***************************	System.out.println("LEVEL-TWO: " + levelTwo.get(x));
				/***************************}
				/***************************System.out.println("********************************");*/

				// try to populate level three
				ArrayList<String> levelThree = new ArrayList<String>();
				for(int j=0; j<levelTwo.size(); j++)
				{
					// connect it to its parent node
					root.getChildren().get(i).addChild(new Node(levelTwo.get(j)));
					root.getChildren().get(i).getChildren().get(j).setLines(text, this.limit2);

					// find children candidates on WAN list
					levelThree = wan.matchTargets(levelTwo.get(j));

					/***************************System.out.println("********************TESTING LEVEL THREE ASSOCIATIONS***********");
					/***************************System.out.println(levelOne.get(i) + " LINKS TO:");
					/***************************System.out.println("***" + levelTwo.get(j) + " LINKS TO:");
					/***************************for(int x=0; x<levelThree.size(); x++)
					/***************************{
					/***************************	System.out.println("LEVEL-THREE: " + levelThree.get(x));
					/***************************}
					/***************************System.out.println("********************************");*/

					levelThree = filter.crossList(levelThree,kigo);

					// filter out any words that duplicate their grandparent node
					levelThree = filter.crossGrandparent(levelThree,levelOne.get(i));

					/***************************System.out.println("********************TESTING LEVEL THREE VS LEVEL ONE***********");
					/***************************System.out.println(levelOne.get(i) + " LINKS TO:");
					/***************************System.out.println("***" + levelTwo.get(j) + " LINKS TO:");
					/***************************for(int x=0; x<levelThree.size(); x++)
					/***************************{
					/***************************	System.out.println("LEVEL-THREE: " + levelThree.get(x));
					/***************************}
					/***************************System.out.println("********************************");*/

					// filter any nodes that duplicate any grandparent's children
					levelThree = filter.compareKeywords(levelThree,levelTwo);

					/***************************System.out.println("********************TESTING LEVEL THREE VS LEVEL TWO***********");
					/***************************System.out.println(levelOne.get(i) + " LINKS TO:");
					/***************************System.out.println("***" + levelTwo.get(j) + " LINKS TO:");
					/***************************for(int x=0; x<levelThree.size(); x++)
					/***************************{
					/***************************	System.out.println("LEVEL-THREE: " + levelThree.get(x));
					/***************************}
					/***************************System.out.println("********************************");*/

					// filter out any words not found in source text
					levelThree = filter.crossList(levelThree,text.getTokens());

					/***************************System.out.println("********************TESTING LEVEL THREE VS TEXT***********");
					/***************************System.out.println(levelOne.get(i) + " LINKS TO:");
					/***************************System.out.println("***" + levelTwo.get(j) + " LINKS TO:");
					/***************************for(int x=0; x<levelThree.size(); x++)
					/***************************{
					/***************************	System.out.println("LEVEL-THREE: " + levelThree.get(x));
					/***************************}
					/***************************System.out.println("********************************");*/


					// add remaining words to the tree
					for(int n=0; n<levelThree.size(); n++)
					{
						root.getChildren().get(i).getChildren().get(j).addChild(new Node(levelThree.get(n)));
						root.getChildren().get(i).getChildren().get(j).getChildren().get(n).setLines(text, this.limit13);
					}
				}
			}
		}
	}

	// remove leaf nodes that are not at level three
	public void prune()
	{
		// iterating over level one
		for(int i=0; i<root.getChildren().size(); i++)
		{
			// for each level one node, iterate over its children
			for(int j=0; j<root.getChildren().get(i).getChildren().size(); j++)
			{
				// if a level two node has no children, remove it
				if(root.getChildren().get(i).getChildren().get(j).getChildren().isEmpty())
				{
					root.getChildren().get(i).getChildren().remove(j);
					j--;
				}
			}

			// if a level one node has no children, remove it
			if(root.getChildren().get(i).getChildren().isEmpty())
			{
				root.getChildren().remove(i);
				i--;
			}
		}
	}
}

// will filter out invalid nodes:
// nodes duplicating grandparents or grandparent's children nodes
// nodes that are not contained in source text
// nodes that are not contained in WAN cues list
class NodeFilter
{
	// compares list entries to the given text
	// removes entries not found in text
	public ArrayList<String> crossSourceText(ArrayList<String> list, String text)
	{
		// iterate through the list
		for(int i=0; i<list.size(); i++)
		{
			// if word not found in text
			// remove entry and shift index one step back
			if(!text.contains(list.get(i)))
			{
				list.remove(i);
				i--;
			}
		}
		return list;
	}

	// compares two lists
	// removes entries from 1st list that are not on the second list
	public ArrayList<String> crossList(ArrayList<String> list1, ArrayList<String> list2)
	{

		// iterate over first list
		for(int i=0; i<list1.size(); i++)
		{
			boolean found = false;
			// iterate over second list
			for(int j=0; j<list2.size(); j++)
			{
				// compare entry from first list to an entry from second list
				if(list2.get(j).equals(list1.get(i)))
				{
					// switch boolean value and stop iterating
					found = true;
					break;
				}
			}
			// if a match was not found
			// remove entry from first list and shift index one step back
			if(!found)
			{
				list1.remove(i);
				i--;
			}
		}
		return list1;
	}

	// compares level three nodes against their grandparent nodes
	// eliminates level three node that is a duplicate of the grandparent node
	public ArrayList<String> crossGrandparent(ArrayList<String> list, String text)
	{
		// iterate through the list
		for(int i=0; i<list.size(); i++)
		{
			// if word not found in text
			// remove entry and shift index one step back
			if(text.contains(list.get(i)))
			{
				list.remove(i);
				i--;
			}
		}
		return list;
	}

	// compares node candidates
	// eliminates grandchildren that duplicate their grandparent
	// eliminates children that duplicate their parent's siblings from the same grandparent
	public ArrayList<String> compareKeywords(ArrayList<String> candidates, ArrayList<String> existing)
	{
		boolean found = false;
		// iterate over first list
		for(int i=0; i<candidates.size(); i++)
		{
			// iterate over second list
			for(int j=0; j<existing.size(); j++)
			{
				// compare entry from first list to an entry from second list
				if(existing.get(j).equals(candidates.get(i)))
				{
					// switch boolean value and stop iterating
					found = true;
					break;
				}
			}
			// if a match was not found
			// remove entry from first list and shift index one step back
			if(found)
			{
				candidates.remove(i);
				i--;
			}
		}
		return candidates;
	}
}

// a node on the keyword tree
// consists of a word field, link to parent node, and links to children nodes
class Node
{
	// stored word
	private String label;
	// lines at specified locations
	private ArrayList<String> lines;
	// keyword of the previous level
	private Node parent;
	// keywords of the next level
	private ArrayList<Node> children;

	public Node(String s)
	{
		this.label = s;
		this.lines = new ArrayList<String>();
		this.children = new ArrayList<Node>();
	}

	// getters and setters
	public String getLabel()
	{
		return this.label;
	}

	public void setLines(TextProcessor text, int limit)
	{
		this.lines = text.fetchLines(this.label, limit);
	}

	public ArrayList<String> getLines()
	{
		return this.lines;
	}

	public void setParent(Node p)
	{
		this.parent = p;
	}

	public Node getParent()
	{
		return this.parent;
	}

	// add the given child node to this node
	// assign this node as the given node's parent
	public void addChild(Node n)
	{
		this.children.add(n);
		n.setParent(this);
	}

	public ArrayList<Node> getChildren()
	{
		return this.children;
	}
}

////////////////////////////////////////////////////////////////////////////////
// stores word association norms
// cue list for cue words
// targets list for associations
class WordAssociations
{
	private ArrayList<String> cues;
	private ArrayList<String> targets;
	// read WAN list, line at a time
	// discard unnecessary data
	// result: word pair cue+target per list entry
	public WordAssociations(String file) throws IOException
	{
		targets = new ArrayList<String>();
		cues = new ArrayList<String>();
		try (BufferedReader reader = new BufferedReader(new FileReader(file)))
		{
			// skip the header
			String l = reader.readLine();
		    while((l = reader.readLine()) != null)
		    {
				// store one word pair per index
				this.cues.add((l.substring(0,l.indexOf(',')).toLowerCase()));
				this.targets.add(l.substring(l.indexOf(" ")+1,l.indexOf(",",l.indexOf(" "))).toLowerCase());
			}
		}
	}

	// getters
	public ArrayList<String> getTargets()
	{
		return this.targets;
	}

	public ArrayList<String> getCues()
	{
		return this.cues;
	}

	// return target words for the given cue word
	public ArrayList<String> matchTargets(String key)
	{
		ArrayList<String> a = new ArrayList<String>();
		// iterate over the lists
		for(int i=0; i<this.targets.size(); i++)
		{
			// store target words in the list if cue word is the given word
			if(this.cues.get(i).equals(key))
			{
				a.add(this.targets.get(i));
			}
			else if(this.cues.get(i).compareTo(key)>0)
			{
				break;
			}
		}
		return a;
	}
}