package debugrecorder.views;



public class FileInfo 
{
	public String FileName;
//	public int LineNumber;
//	public String StatementText;
	public int Rank;
	public double Suspiciousness;
	
	public static FileInfo ImportFileInfo(String line)
	{
		if( line.isEmpty() )
			return null;
		String[] data = line.split("\t");
		if( data.length != 3)
			return null;
		
		FileInfo info = new FileInfo();
		info.FileName = data[1];
//		info.LineNumber = Integer.parseInt(data[1]);
//		info.StatementText = data[2];
		info.Rank = Integer.parseInt(data[0]);
		info.Suspiciousness = Double.parseDouble(data[2]);
		
		return info;
	}
	

}
