/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package de.aulersipel.util;

import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import org.compiere.model.I_C_BankAccount;
import org.compiere.model.I_C_PaySelection;
import org.compiere.model.MClient;
import org.compiere.model.MCurrency;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.model.MSysConfig;
import org.compiere.util.CLogger;
import org.compiere.util.PaymentExport;
import org.compiere.util.DB;
import org.compiere.util.Env;


//warum wurde der source nicht �bernommen
/**
 * 	SEPA Payment Export
 *  based on generic export example
 *	
 * 	@author 	integratio/pb
 * 
 */
public class as1SEPAPaymentExport implements PaymentExport
{
	/** Logger								*/
	static private CLogger	s_log = CLogger.getCLogger (as1SEPAPaymentExport.class);


	/** BPartner Info Index for Value       */
	private static final int     BP_VALUE = 0;
	/** BPartner Info Index for Name        */
	private static final int     BP_NAME = 1;
	/** BPartner Info Index for Contact Name    */
	private static final int     BP_CONTACT = 2;
	/** BPartner Info Index for Address 1   */
	private static final int     BP_ADDR1 = 3;
	/** BPartner Info Index for Address 2   */
	private static final int     BP_ADDR2 = 4;
	/** BPartner Info Index for City        */
	private static final int     BP_CITY = 5;
	/** BPartner Info Index for Region      */
	private static final int     BP_REGION = 6;
	/** BPartner Info Index for Postal Code */
	private static final int     BP_POSTAL = 7;
	/** BPartner Info Index for Country     */
	private static final int     BP_COUNTRY = 8;
	/** BPartner Info Index for Reference No    */
	private static final int     BP_REFNO = 9;

	private static String DocumentType="pain.001.002.03";

	/**************************************************************************
	 *  Export to File
	 *  @param checks array of checks
	 *  @param file file to export checks
	 *  @return number of lines
	 */
	public int exportToFile (MPaySelectionCheck[] checks, File file, StringBuffer err)
	{
		int xx=0;
		if (checks == null || checks.length == 0)
			return 0;
		//  Must be a file
		if (file.isDirectory())
		{
			err.append("No se puede escribir, el archivo seleccionado es un directorio - " + file.getAbsolutePath());
			s_log.log(Level.SEVERE, err.toString());
			return -1;
		}
		//  delete if exists
		try
		{
			if (file.exists())
				file.delete();
		}
		catch (Exception e)
		{
			s_log.log(Level.WARNING, "Could not delete - " + file.getAbsolutePath(), e);
		}

		char x = '"';      //  ease
		int noLines = 0;
		StringBuffer line = null;
		try
		{
			FileWriter fw = new FileWriter(file);

			
			StringBuffer msg = null;
			msg = new StringBuffer();
			
			//StringBuffer msg=null;
			
			
			MClient adc = new MClient(Env.getCtx(),Env.getAD_Client_ID(Env.getCtx()),null);

			
			String MsgId="Message-xxxxx";
			String CreationDate="2011-12-32T09:30:47.000Z";
			int    NumberOfTransactions=0;
			String InitiatorName=adc.getName();
				String PaymentInfoId="Payments";
			BigDecimal CtrlSum = BigDecimal.valueOf(6554.23);
			String ExecutionDate = "2011-12-31";
			String Dbtr_Name = adc.getName();
			String DbtrAcct_IBAN = "IBAN_IBAN";
			String DbtrAcct_BIC = "BICBIC";
			
			CtrlSum=BigDecimal.ZERO;
			for (int i = 0; i < checks.length; i++)
			{
				MPaySelectionCheck mpp = checks[i];
				CtrlSum=CtrlSum.add(mpp.getPayAmt());
				NumberOfTransactions++;
			}
			MPaySelectionCheck mppC = checks[0];
			I_C_PaySelection ps = mppC.getC_PaySelection();
			
			MsgId = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(ps.getCreated());
			
			ExecutionDate = new SimpleDateFormat("yyyy-MM-dd").format(ps.getPayDate());
			CreationDate = new SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis())
								+"T"
								+new SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())
								+".000Z";
			
			I_C_BankAccount cba =ps.getC_BankAccount();
			DbtrAcct_IBAN = cba.getIBAN();
			DbtrAcct_BIC = cba.getC_Bank().getRoutingNo();
			
			msg.append(iSEPA_Header());
			msg.append(iSEPA_Document_open());
			msg.append(iSEPA_CstmrCdtTrfInitn_open());
				msg.append(iSEPA_GrpHdr_open());
					msg.append(iSEPA_MsgId(iSEPA_ConvertSign(MsgId)));
					msg.append(iSEPA_CreDtTm(iSEPA_ConvertSign(CreationDate)));
					msg.append(iSEPA_NbOfTxs(NumberOfTransactions));
					msg.append(iSEPA_InitgPty(iSEPA_ConvertSign(InitiatorName)));
				msg.append(iSEPA_GrpHdr_close());
				msg.append(iSEPA_PmtInf_open());
					msg.append(iSEPA_PmtInfId(iSEPA_ConvertSign(PaymentInfoId)));
					msg.append(iSEPA_PmtMtd());
					msg.append(iSEPA_BtchBookg());
					msg.append(iSEPA_NbOfTxs(NumberOfTransactions));
					msg.append(iSEPA_CtrlSum(CtrlSum));
					msg.append(iSEPA_PmtTpInf());
					msg.append(iSEPA_ReqdExctnDt(iSEPA_ConvertSign(ExecutionDate)));
					msg.append(iSEPA_DbtrNm(iSEPA_ConvertSign(Dbtr_Name)));
					msg.append(iSEPA_DbtrAcctIBAN(iSEPA_ConvertSign(DbtrAcct_IBAN)));
					msg.append(iSEPA_DbtrAgtFinInstnIdBIC(iSEPA_ConvertSign(DbtrAcct_BIC)));
					msg.append(iSEPA_ChrgBr());
					
					for (int i = 0; i < checks.length; i++)
					{
						MPaySelectionCheck mpp = checks[i];

						if (mpp == null)
							continue;
						mpp.getC_BPartner_ID();
						mpp.getDocumentNo();
						mpp.isProcessed();
						String PmtId=String.valueOf(adc.getAD_Client_ID())+"-"+String.valueOf(mpp.get_ID());
						//"OriginatorID1235";
						String Ccy="EUR";
						BigDecimal Amount=BigDecimal.ZERO;
						String AmountAsString ="";
						AmountAsString = String.valueOf(Amount);
						String CreditorName="aaaaaaaaa";
						String CdtrAcct_BIC="cdtr bic";
						String CdtrAcct_IBAN="cdtr iban";
						
						String bp[] = getBPartnerInfo(mpp.getC_BPartner_ID());
						CreditorName = bp[BP_NAME];
						String ba[] = getBPartnerAccountInfo(mpp.getC_BPartner_ID());
						CdtrAcct_IBAN = ba[0];
						CdtrAcct_BIC = ba[1];
						
						//trx iteration
						msg=msg.append(iSEPA_CdtTrfTxInf_open());
							msg=msg.append(iSEPA_PmtId(iSEPA_ConvertSign(PmtId)));
							msg=msg.append(iSEPA_AmtInstdAmt(MCurrency.getISO_Code(Env.getCtx(), mpp.getParent().getC_Currency_ID()),String.valueOf(mpp.getPayAmt())));
							msg=msg.append(iSEPA_CdtrAgtFinInstnIdBIC(iSEPA_ConvertSign(CdtrAcct_BIC)));
							msg=msg.append(iSEPA_CdtrNm(iSEPA_ConvertSign(CreditorName)));
							msg=msg.append(iSEPA_CdtrAcctIBAN(iSEPA_ConvertSign(CdtrAcct_IBAN)));
						msg=msg.append(iSEPA_CdtTrfTxInf_close());
					}
					
					
					msg=msg.append(iSEPA_PmtInf_close());
				msg=msg.append(iSEPA_CstmrCdtTrfInitn_close());
			msg=msg.append(iSEPA_Document_close());
			// convert everthing to utf-8
			//String msg_utf8 = new String(msg.toString().getBytes("UTF-8"), "ISO-8859-1");
			//fw.write(iSEPA_ConvertSign(msg.toString()));
			fw.write(msg.toString());

			fw.flush();
			fw.close();
			noLines=NumberOfTransactions;
		}
		catch (Exception e)
		{
			err.append(e.toString());
			s_log.log(Level.SEVERE, "", e);
			return -1;
		}

		return noLines;
	}   //  exportToFile

	

	/**
	 *  Get Vendor/Customer Bank Account Information
	 *  Based on BP_ static variables
	 *  @param C_BPartner_ID BPartner
	 *  @return info array
	 */
	private static String[] getBPartnerAccountInfo (int C_BPartner_ID)
	{
		String[] ba = new String[2];
/*** as1 changed to simplify account management
		String sql = "select ba.accountno,b.routingno "
					+"from c_bp_bankaccount ba,c_bank b "
					+"where ba.c_bpartner_id=? "
					+"and ba.c_bank_id = b.c_bank_id " 
					;
****/	
		String sql = "select ba.accountno,ba.routingno "
				+"from c_bp_bankaccount ba "
				+"where ba.c_bpartner_id=? "
				;
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, C_BPartner_ID);
			rs = pstmt.executeQuery();
			//
			if (rs.next())
			{
				ba[0] = rs.getString(1);
				if (ba[0] == null)
					ba[0] = "";
				ba[1] = rs.getString(2);
				if (ba[1] == null)
					ba[1] = "";
			}
		}
		catch (SQLException e)
		{
			s_log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return ba;
	}   //  getBPartnerAccountInfo


	
	
	
	
	/**
	 *  Get Customer/Vendor Info.
	 *  Based on BP_ static variables
	 *  @param C_BPartner_ID BPartner
	 *  @return info array
	 */
	private static String[] getBPartnerInfo (int C_BPartner_ID)
	{
		String[] bp = new String[10];

		String sql = "SELECT bp.Value, bp.Name, c.Name AS Contact, "
			+ "a.Address1, a.Address2, a.City, r.Name AS Region, a.Postal, "
			+ "cc.Name AS Country, bp.ReferenceNo "
			+ "FROM C_BPartner bp, AD_User c, C_BPartner_Location l, C_Location a, C_Region r, C_Country cc "
			+ "WHERE bp.C_BPartner_ID=?"        // #1
			+ " AND bp.C_BPartner_ID=c.C_BPartner_ID(+)"
			+ " AND bp.C_BPartner_ID=l.C_BPartner_ID"
			+ " AND l.C_Location_ID=a.C_Location_ID"
			+ " AND a.C_Region_ID=r.C_Region_ID(+)"
			+ " AND a.C_Country_ID=cc.C_Country_ID "
			+ "ORDER BY l.IsBillTo DESC";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, C_BPartner_ID);
			rs = pstmt.executeQuery();
			//
			if (rs.next())
			{
				bp[BP_VALUE] = rs.getString(1);
				if (bp[BP_VALUE] == null)
					bp[BP_VALUE] = "";
				bp[BP_NAME] = rs.getString(2);
				if (bp[BP_NAME] == null)
					bp[BP_NAME] = "";
				bp[BP_CONTACT] = rs.getString(3);
				if (bp[BP_CONTACT] == null)
					bp[BP_CONTACT] = "";
				bp[BP_ADDR1] = rs.getString(4);
				if (bp[BP_ADDR1] == null)
					bp[BP_ADDR1] = "";
				bp[BP_ADDR2] = rs.getString(5);
				if (bp[BP_ADDR2] == null)
					bp[BP_ADDR2] = "";
				bp[BP_CITY] = rs.getString(6);
				if (bp[BP_CITY] == null)
					bp[BP_CITY] = "";
				bp[BP_REGION] = rs.getString(7);
				if (bp[BP_REGION] == null)
					bp[BP_REGION] = "";
				bp[BP_POSTAL] = rs.getString(8);
				if (bp[BP_POSTAL] == null)
					bp[BP_POSTAL] = "";
				bp[BP_COUNTRY] = rs.getString(9);
				if (bp[BP_COUNTRY] == null)
					bp[BP_COUNTRY] = "";
				bp[BP_REFNO] = rs.getString(10);
				if (bp[BP_REFNO] == null)
					bp[BP_REFNO] = "";
			}
		}
		catch (SQLException e)
		{
			s_log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return bp;
	}   //  getBPartnerInfo

	private  static String iSEPA_ConvertToSEPASign(String convertit)
	{
		MSysConfig sc = new MSysConfig(Env.getCtx(), 0, null); 
		String sepaconvert=MSysConfig.getValue("iSEPA_SPECIAL_SIGN", Env.getAD_Client_ID(Env.getCtx()));
		Integer sc_length=sepaconvert.length();
		
		String[] TokenList = sepaconvert.split(",");
		
		
		String targettext = "";
		for (int ii=0 ; ii<TokenList.length;ii++)
		{
			int tlength = TokenList[ii].length();
			if (tlength>2 && TokenList[ii].substring(1,2).equals(":"))
			{
				if (convertit.equals(TokenList[ii].substring(0,1)))
				{
					return TokenList[ii].substring(2,TokenList[ii].length());
				}
			}
		}
		return ".";
	}

	public   static String iSEPA_ConvertSign(String text)
	{
		String sepa_zeichen = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789/?:().,'+- ";
		
		int l=text.length();
		String targettext = "";

		for (int i=0; i<l ; i++)
		{
			if (sepa_zeichen.contains(text.substring(i,i+1)))
			{
				targettext = targettext+text.substring(i,i+1);
			}
			else
			{
				targettext = targettext+iSEPA_ConvertToSEPASign(text.substring(i,i+1));	
			}
		}

		
		return targettext;
	}

	
	
	public  static  void SetDocumentType(String dt)
	{
		DocumentType = dt;
	}


	public   static    String iSEPA_CdtrNm(String Name)
	{
		String msg="";
		msg=msg.concat("<Cdtr>");
		msg=msg.concat("<Nm>");
		msg=msg.concat(Name);
		msg=msg.concat("</Nm>");
		msg=msg.concat("</Cdtr>");
		return msg;
	}


	public   static    String iSEPA_RmtInf(String info)
	{
		String msg="";
		msg=msg.concat("<RmtInf>");
		msg=msg.concat("<Nm>");
		msg=msg.concat(info);
		msg=msg.concat("</Nm>");
		msg=msg.concat("</RmtInf>");
		return msg;
	}

	
	public   static String iSEPA_AmtInstdAmt(String ccy,String amount)
	{
		String msg="";
		msg=msg.concat("<Amt>");
		msg=msg.concat("<InstdAmt ")
			.concat("Ccy=\"")
			.concat(ccy)
			.concat("\">");
		msg=msg.concat(amount);
		msg=msg.concat("</InstdAmt>");
		msg=msg.concat("</Amt>");
		return msg;
	}

	public  static  String iSEPA_PmtId(String id)
	{
		String msg="";
		msg=msg.concat("<PmtId>");
		msg=msg.concat("<EndToEndId>");
		msg=msg.concat(id);
		msg=msg.concat("</EndToEndId>");
		msg=msg.concat("</PmtId>");
		return msg;
	}
	public   static String iSEPA_ChrgBr()
	{
		String msg="";
		msg=msg.concat("<ChrgBr>");
		msg=msg.concat("SLEV");
		msg=msg.concat("</ChrgBr>");
		return msg;
	}

	public  static  String iSEPA_CdtrAgtFinInstnIdBIC(String BIC)
	{
		String msg="";
		msg=msg.concat("<CdtrAgt>");
		msg=msg.concat("<FinInstnId>");
		msg=msg.concat("<BIC>");
		msg=msg.concat(BIC);
		msg=msg.concat("</BIC>");
		msg=msg.concat("</FinInstnId>");
		msg=msg.concat("</CdtrAgt>");
		return msg;
	}

	public   static String iSEPA_DbtrAgtFinInstnIdBIC(String BIC)
	{
		String msg="";
		msg=msg.concat("<DbtrAgt>");
		msg=msg.concat("<FinInstnId>");
		msg=msg.concat("<BIC>");
		msg=msg.concat(BIC);
		msg=msg.concat("</BIC>");
		msg=msg.concat("</FinInstnId>");
		msg=msg.concat("</DbtrAgt>");
		return msg;
	}

	public   static String iSEPA_CdtrAcctIBAN(String IBAN)
	{
		String msg="";
		msg=msg.concat("<CdtrAcct>");
		msg=msg.concat("<Id>");
		msg=msg.concat("<IBAN>");
		msg=msg.concat(IBAN);
		msg=msg.concat("</IBAN>");
		msg=msg.concat("</Id>");
		msg=msg.concat("</CdtrAcct>");
		return msg;
	}

	public   static String iSEPA_DbtrAcctIBAN(String IBAN)
	{
		String msg="";
		msg=msg.concat("<DbtrAcct>");
		msg=msg.concat("<Id>");
		msg=msg.concat("<IBAN>");
		msg=msg.concat(IBAN);
		msg=msg.concat("</IBAN>");
		msg=msg.concat("</Id>");
		msg=msg.concat("</DbtrAcct>");
		return msg;
	}

	public   static String iSEPA_DbtrNm(String Name)
	{
		String msg="";
		msg=msg.concat("<Dbtr>");
		msg=msg.concat("<Nm>");
		msg=msg.concat(Name);
		msg=msg.concat("</Nm>");
		msg=msg.concat("</Dbtr>");
		return msg;
	}

	public  static  String iSEPA_MsgId(String msgid)
	{
		String msg="";
		msg=msg.concat("<MsgId>");
		msg=msg.concat(msgid);
		msg=msg.concat("</MsgId>");
		return msg;
	}

	public  static  String iSEPA_ReqdExctnDt(String date)
	{
		String msg="";
		msg=msg.concat("<ReqdExctnDt>");
		msg=msg.concat(date);
		msg=msg.concat("</ReqdExctnDt>");
		return msg;
	}




	public   static String iSEPA_CreDtTm(String date)
	{
		String msg="";
		msg=msg.concat("<CreDtTm>");
		msg=msg.concat(date);
		msg=msg.concat("</CreDtTm>");
		return msg;
	}
	

	public  static  String iSEPA_NbOfTxs(int number)
	{
		String msg="";
		msg=msg.concat("<NbOfTxs>");
		msg=msg.concat(String.valueOf(number));
		msg=msg.concat("</NbOfTxs>");
		return msg;
	}
	
	public  static  String iSEPA_InitgPty(String InitiatorName)
	{
		String msg="";
		msg=msg.concat("<InitgPty>");
		msg=msg.concat("<Nm>");
		msg=msg.concat(InitiatorName);
		msg=msg.concat("</Nm>");
		msg=msg.concat("</InitgPty>");
		return msg;
	}
	

	public  static  String iSEPA_CtrlSum(BigDecimal CtrlSum)
	{
		String msg="";
		String amount=String.valueOf(CtrlSum);
		//verify if thousand delimiter are removed
		//verify if decimal point is a point
		
		msg=msg.concat("<CtrlSum>");
		msg=msg.concat(amount);
		msg=msg.concat("</CtrlSum>");
		return msg;
	}
	
	public   static String iSEPA_PmtMtd()
	{
		String msg="";
		msg=msg.concat("<PmtMtd>");
		msg=msg.concat("TRF");
		msg=msg.concat("</PmtMtd>");
		return msg;
	}
	
	public  static  String iSEPA_BtchBookg()
	{
		String msg="";
		msg=msg.concat("<BtchBookg>");
		msg=msg.concat("true");
		msg=msg.concat("</BtchBookg>");
		return msg;
	}
	public   static String iSEPA_PmtInfId(String PaymentInfoId)
	{
		String msg="";
		msg=msg.concat("<PmtInfId>");
		msg=msg.concat(PaymentInfoId);
		msg=msg.concat("</PmtInfId>");
		return msg;
	}
	public  static  String iSEPA_Header()
	{
		String msg = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
		return msg;
	}
	
	
	public   static String iSEPA_Document_open()
	{
		String msg="";
		
		msg=msg.concat("<Document  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"");
		msg=msg.concat(" xmlns=\"urn:iso:std:iso:20022:tech:xsd:");
		msg=msg.concat(DocumentType);
		msg=msg.concat("\">");
		return msg;
	}
	public   static String iSEPA_Document_close()
	{
		String msg="</Document>";
		return msg;
	}

	public   static String iSEPA_CdtTrfTxInf_open()
	{
		String msg="";
		msg=msg.concat("<CdtTrfTxInf>");
		return msg;
	}
	

	public  static  String iSEPA_CdtTrfTxInf_close()
	{
		String msg="";
		msg=msg.concat("</CdtTrfTxInf>");
		return msg;
	}
	

	public   static String iSEPA_CstmrCdtTrfInitn_open()
	{
		String msg="";
		msg=msg.concat("<CstmrCdtTrfInitn>");
		return msg;
	}

	public   static String iSEPA_CstmrCdtTrfInitn_close()
	{
		String msg="";
		msg=msg.concat("</CstmrCdtTrfInitn>");
		return msg;
	}
	
	public   static String iSEPA_GrpHdr_open()
	{
		String msg="";
		msg=msg.concat("<GrpHdr>");
		return msg;
	}

	public   static String iSEPA_GrpHdr_close()
	{
		String msg="";
		msg=msg.concat("</GrpHdr>");
		return msg;
	}
	
	public  static  String iSEPA_PmtInf_open()
	{
		String msg="";
		msg=msg.concat("<PmtInf>");
		return msg;
	}
	public   static String iSEPA_PmtInf_close()
	{
		String msg="";
		msg=msg.concat("</PmtInf>");
		return msg;
	}
	
	
	public  static  String iSEPA_PmtTpInf()
	{
		String msg="";
		msg=msg.concat("<PmtTpInf>");
		msg=msg.concat("<SvcLvl>");
		msg=msg.concat("<Cd>");
		msg=msg.concat("SEPA");
		msg=msg.concat("</Cd>");
		msg=msg.concat("</SvcLvl>");
		msg=msg.concat("</PmtTpInf>");
		return msg;
	}
	

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}	//	PaymentExport
