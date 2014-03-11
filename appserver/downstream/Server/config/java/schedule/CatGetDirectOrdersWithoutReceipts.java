package config.java.schedule;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.core.ListUtil;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class CatGetDirectOrdersWithoutReceipts
    extends ScheduledTask {
    public void run() throws ScheduledTaskException  {
        ariba.base.core.Log.customer.debug("Beginning GetDirectOrdersWithoutReceipts program .....");

        Partition partition;
        partition = Base.getService().getPartition("mfg1");
        String queryTxt;
        AQLQuery query;
        AQLOptions options;
        AQLResultCollection results;
        List queryResults;
        List mailList;

        try
            {
            queryTxt = "select UniqueName from ariba.purchasing.core.DirectOrder"
                           + " where (CurrentDate() - OrderedDate) < 5 and UniqueName NOT IN"
                           + "(select distinct UniqueName from ariba.purchasing.core.DirectOrder where Receipts is not null)";

            query = AQLQuery.parseQuery(queryTxt);
            options = new AQLOptions(partition);
            results = Base.getService().executeQuery(query, options);
            queryResults = ListUtil.list();
            mailList = getMailList();



            if (results.getErrors() != null) {
                ariba.base.core.Log.customer.debug("ERROR GETTING RESULTS");
                }

            while (results.next()) {
                String displayUniqueName = results.getString(0);
                ariba.base.core.Log.customer.debug(displayUniqueName);
                ariba.base.core.Log.customer.debug("creating list..");
                ListUtil.addElementIfAbsent(queryResults, displayUniqueName);
                } //Storing results of query in List queryResults

            Properties props = System.getProperties();
            props.setProperty("mail.transport.protocol", "smtp");
            String smtpurl = Base.getService().getParameter(null, "System.Base.SMTPServerName");
            props.put("mail.smtp.host", smtpurl);
            Session session     = Session.getDefaultInstance(props, null);
            MimeMessage message = new MimeMessage(session);

            //if (results != null) {

            if ( ListUtil.getListSize(queryResults)	>0){
                ariba.base.core.Log.customer.debug("results are there....");


                message.setFrom(new InternetAddress("NoReceiptNotifier@cat.com")); //Sender's email address //

                //message.addRecipient(Message.RecipientType.TO,new InternetAddress("anoma.shah@in.ibm.com"));     //Receiver's email address

                InternetAddress [] addressTo = new InternetAddress[mailList.size()];

                for (int i = 0; i < mailList.size(); i++) {
                    addressTo[i] = new InternetAddress(mailList.get(i).toString());
                    ariba.base.core.Log.customer.debug("Email set to " +mailList.get(i).toString());

				}
                    message.setRecipients(Message.RecipientType.TO, addressTo);
              		message.setSubject("Orders without receipts");                    //Subject of Mail
                	ariba.base.core.Log.customer.debug("separting query result with \n");
                	message.setText("There are orders which have no receipts created.The orders are listed below:" + "\n"
                                    + ListUtil.listToString(queryResults, "\n")); //Mail Content
                	ariba.base.core.Log.customer.debug("printing mail list as body of message");
                	ariba.base.core.Log.customer.debug("sending mail.....");
                	Transport.send(message);
            		ariba.base.core.Log.customer.debug("mail sent");

				}

            else{

				ariba.base.core.Log.customer.debug("####### No Orders found , No notificatin mails sent..#######");

				}
            }
        catch (Exception e) {
            ariba.base.core.Log.customer.debug(e.toString());
            return;
            }
        }

    public CatGetDirectOrdersWithoutReceipts() { }

    List getMailList() {
        List mailList = new ArrayList();

        try {
            FileReader fr      = new FileReader("/msc/arb821/Server/config/variants/vcsv2/data/CatOrdersNotifierEmailList.txt");
            BufferedReader bin = new BufferedReader(fr);

            String str;


            while ((str = bin.readLine()) != null) {
				ariba.base.core.Log.customer.debug("Adding Email id to  mailList");
                mailList.add(str);
                }

            bin.close();
            }
        catch (IOException e) {
            e.printStackTrace();
            }

        return mailList;
        }
    }
