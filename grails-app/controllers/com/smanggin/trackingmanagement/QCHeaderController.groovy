package com.smanggin.trackingmanagement



import grails.converters.JSON
import org.springframework.dao.DataIntegrityViolationException

/**
 * QCHeaderController
 * A controller class handles incoming web requests and performs actions such as redirects, rendering views and so on.
 */

class QCHeaderController {
    def globalService
    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index() {
        redirect(action: "list", params: params)
    }

    def list() {
        params.max = Math.min(params.max ? params.int('max') : 10, 100)
        def results = QCHeader.createCriteria().list(params){}
        [QCHeaderInstanceList: results, QCHeaderInstanceTotal: results.totalCount]
    }

    def create() {
        [QCHeaderInstance: new QCHeader(params)]
    }

    def save() {
        println "params " + params 
        def QCHeaderInstance = new QCHeader(params)
        QCHeaderInstance.properties = params
        QCHeaderInstance.createdBy= session.user
        QCHeaderInstance.gallon = Gallon.findByCode(params.gallon?.code)
        QCHeaderInstance.plant = Plant.findByServerId(params.plant?.serverId)
        QCHeaderInstance.workCenter = WorkCenter.findByServerId(params?.workCenter?.serverId)
        QCHeaderInstance.transactionGroup =TransactionGroup.findByServerId(params?.transactionGroup?.serverId)


        if (!QCHeaderInstance.save(flush: true)) {
            render(view: "create", model: [QCHeaderInstance: QCHeaderInstance])
            return
        }

		flash.message = message(code: 'default.created.message', args: [message(code: 'QCHeader.label', default: 'QCHeader'), QCHeaderInstance.id])
        redirect(action: "edit", params: [serverId:QCHeaderInstance?.serverId])
    }

    def show() {
        def QCHeaderInstance = QCHeader.findByServerId(params.serverId)
        if (!QCHeaderInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'QCHeader.label', default: 'QCHeader'), params.id])
            redirect(action: "list")
            return
        }

        def processQC = ProcessQC.findAllByProcess(QCHeaderInstance.workCenter?.process)
        println " processQC "+ processQC

        [QCHeaderInstance: QCHeaderInstance,processQCAll:processQC]
    }

    def edit() {
        def QCHeaderInstance = QCHeader.findByServerId(params.serverId)
        if (!QCHeaderInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'QCHeader.label', default: 'QCHeader'), params.id])
            redirect(action: "list")
            return
        }

        def processQC = ProcessQC.findAllByProcess(QCHeaderInstance.workCenter?.process)

        [QCHeaderInstance: QCHeaderInstance,processQCAll:processQC]
    }

    def update() {
        println "update" + params
        def QCHeaderInstance = QCHeader.findByServerId(params.serverId)
        println "QCHeaderInstance " + QCHeaderInstance
        if (!QCHeaderInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'QCHeader.label', default: 'QCHeader'), params.id])
            redirect(action: "list")
            return
        }

        if (params.version) {
            def version = params.version.toLong()
            if (QCHeaderInstance.version > version) {
                QCHeaderInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
                          [message(code: 'QCHeader.label', default: 'QCHeader')] as Object[],
                          "Another user has updated this QCHeader while you were editing")
                render(view: "edit", model: [QCHeaderInstance: QCHeaderInstance])
                return
            }
        }

        QCHeaderInstance.properties = params

        QCHeaderInstance.updatedBy= session.user
        QCHeaderInstance.gallon = Gallon.findByCode(params.gallon?.code)
        QCHeaderInstance.plant = Plant.findByServerId(params.plant?.serverId)
        QCHeaderInstance.workCenter = WorkCenter.findByServerId(params?.workCenter?.serverId)
        QCHeaderInstance.transactionGroup =TransactionGroup.findByServerId(params?.transactionGroup?.serverId)
        QCHeaderInstance.qcActions = QCActions.findByServerId(params.qcActions?.serverId)

        

        if (!QCHeaderInstance.save(flush: true)) {
            println "<<<<<<<<<<<<<<<<<  errors >>>>>>>>>>>>> "
            render(view: "edit", model: [QCHeaderInstance: QCHeaderInstance])
            return
        }

        def processQCAll = ProcessQC.findAllByProcess(QCHeaderInstance.workCenter?.process)
        
        processQCAll.each{ processQC -> 
           
            processQC?.qcMaster?.qCQuestions.each{
                if(it.parameterType == 2){
                    println " >>>>>>>>>>>>>>>>>> parameterType 2"
                    params."${it.qCMaster?.code}_${it.sequenceNo}".each{ option->
                        def qcDetail = new QCDetail()
                        qcDetail.qcHeader = QCHeaderInstance
                        qcDetail.qcMaster = it.qCMaster
                        qcDetail.qcQuestions = it
                        qcDetail.results = option
                        qcDetail.createdBy = session.user
                        if(!qcDetail.save(flush:true)){
                            println "erorr" +qcDetail.errors
                        }    
                    }
                }else{
                    def qcDetail = new QCDetail()
                    qcDetail.qcHeader = QCHeaderInstance
                    qcDetail.qcMaster = it.qCMaster
                    qcDetail.qcQuestions = it
                    qcDetail.results = params."${it.qCMaster?.code}_${it.sequenceNo}"
                    qcDetail.createdBy = session.user
                    if(!qcDetail.save(flush:true)){
                        println "erorr" +qcDetail.errors
                    }
                }
                
            }
        }
        
        insertLineBalance(QCHeaderInstance)

		flash.message = message(code: 'default.updated.message', args: [message(code: 'QCHeader.label', default: 'QCHeader'), QCHeaderInstance.id])
        redirect(action: "show", params: [serverId:QCHeaderInstance?.serverId])
    }

    def delete() {
        def QCHeaderInstance = QCHeader.get(params.id)
        if (!QCHeaderInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'QCHeader.label', default: 'QCHeader'), params.id])
            redirect(action: "list")
            return
        }

        try {
            QCHeaderInstance.delete(flush: true)
			flash.message = message(code: 'default.deleted.message', args: [message(code: 'QCHeader.label', default: 'QCHeader'), params.id])
            redirect(action: "list")
        }
        catch (DataIntegrityViolationException e) {
			flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'QCHeader.label', default: 'QCHeader'), params.id])
            redirect(action: "show", id: params.id)
        }
    }

    def jsave() {
        def QCHeaderInstance = (params.id) ? QCHeader.get(params.id) : new QCHeader()
        
        if (!QCHeaderInstance) {                     
            def error = [message: message(code: 'default.not.found.message', args: [message(code: 'QCHeader.label', default: 'QCHeader'), params.id])]
            render([success: false, messages: [errors:[error]] ] as JSON)       
            return
        }
        
        if (params.version)
        {
            def version = params.version.toLong()
            if (version != null) {
                if (QCHeaderInstance.version > version) {
                    QCHeaderInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
                              [message(code: 'QCHeader.label', default: 'QCHeader')] as Object[],
                              "Another user has updated this QCHeader while you were editing")
                    render([success: false, messages: QCHeaderInstance.errors] as JSON)
                    return
                }
            }            
        }
        
        QCHeaderInstance.properties = params
                       
        if (!QCHeaderInstance.save(flush: true)) {
            render([success: false, messages: QCHeaderInstance.errors] as JSON)
            return
        }
                        
        render([success: true] as JSON)
    }

    def jlist() {
        if(params.masterField){
            def c = QCHeader.createCriteria()
            def results = c.list {
                eq(params.masterField.name+'.id',params.masterField.id.toLong())    
            }
            render results as JSON

        }
        else
        {
            params.max = Math.min(params.max ? params.int('max') : 10, 100)
            render QCHeader.list(params) as JSON           
        }
        
    }   

    def jdelete(Long id) {
        def QCHeaderInstance = QCHeader.get(id)
        if (!QCHeaderInstance)
            render([success: false] as JSON)
        else {
            try {
                QCHeaderInstance.delete(flush: true)             
                render([success: true] as JSON)
            }catch (DataIntegrityViolationException e) {
                render([success: false, error: e.message] as JSON)
            }
        }
    }

    def jshow = {
        def QCHeaderInstance = QCHeader.get(params.id)
        if (!QCHeaderInstance) {
            render(
                message : "QCHeader.not.found",
            ) as JSON

        }
        else {
            render([QCHeaderInstance : QCHeaderInstance ] as JSON)
        }
    }


    def insertLineBalance(QCHeaderInstance){
        params.order = params.order ?: 'desc' 
        params.sort = params.sort ?: 'dateCreated' 
        
        def last = LineBalance.createCriteria().list(params){
           maxResults(1)
        }

        

        def lineBalance = new LineBalance()
        lineBalance.plant = QCHeaderInstance.plant
        lineBalance.line = QCHeaderInstance.workCenter.line
        lineBalance.date = new Date()
        lineBalance.beginQty = last?.endQty[0]?:0
        lineBalance.inQty = 0
        lineBalance.outQty = 1
        lineBalance.endQty = lineBalance.beginQty - 1
        lineBalance.createdBy =session.user
        if(!lineBalance.save(flush:true)){
            println "errors " + lineBalance.errors
        }


    }


    /**
    Report  
    **/
    def report(){
        def view = params.report
        render(view: "${view}")

        /*if(params.report == qcSummary){
            println "======= QC summary ======"
            
            
        } else if(params.report == qcAnalysis) {
            println "======= Qc Annalisys ======"
            views = params.report
            
        } */


    }

    /* QC summary */
    def qcSummary(){
        
        def startDate = globalService.correctDateTime(params.startDate)
        def endDate = globalService.correctDateTime(params.endDate)
        def filterDate = globalService.filterDate(startDate, endDate)
        def line1 = Line.findByServerId(params.line1ServerId)
        def line2 = Line.findByServerId(params.line2Code) 
        def plant = Plant.findByServerId(params.plantServerId)

        def results = QCHeader.createCriteria().list(){
            workCenter{
                eq('line',line1)    
                eq('plant',plant)
            }

//            le('date',filterDate.start)
  //          ge('date',filterDate.end)

        }

        println " results" + results

        def list = []

        results.each{
            
            def hasilQc = hasilQc(it)
            println " hasilQc " + hasilQc
            //it.
            def map = [:]
            map.put('gallonCode',it.gallon?.code)
            map.put('date',it.date)
            map.put('hasilQc',hasilQc)
            map.put('action',it.qcActions?.description?:'')
            map.put('userCreated',it.createdBy)
            
            list.push(map)
        }

        render([success: true ,results:list] as JSON)
    }


    def hasilQc(QCHeaderInstance){
        
        def processQCAll = ProcessQC.findAllByProcess(QCHeaderInstance.workCenter?.process)   
        def listMaster= []
        processQCAll.each{

            def listQuestion = []
            def qcMasters=it.qcMaster
            
            def mapMaster=[:]
            mapMaster.put('name',it.qcMaster?.name)

            
            qcMasters?.qCQuestions?.each{ question ->
                def mapQuestion=[:]
                mapQuestion.put('name',question.parameterDesc)
                
                def results = QCDetail.createCriteria().list(){
                    eq('qcHeader',QCHeaderInstance)
                    eq('qcMaster',qcMasters)
                    eq('qcQuestions',question)
                }

                mapQuestion.put('value',results.results)

                if(results){
                    listQuestion.push(mapQuestion)
                }
            }

            mapMaster.put('value',listQuestion)

            if(listQuestion.size() > 0){
                listMaster.push(mapMaster)    
            }
            
        }

        return listMaster     
    }



}
